import { SensorNode, NodeNetworkBus } from './sensorNode';
import { EdgeAggregator } from './edgeAggregator';
import { MutualExclusionCoordinator } from './mutualExclusion';
import { AggregationGrpcClient } from './grpcClient';
import { InterNodeMessage, SensorReading, ClusterSummaryData, VectorClockMap, SensorType } from './types';
import WebSocket from 'ws';

export interface ClusterConfig {
  clusterId: string;
  clusterName: string;
  nodeIds: string[];
  sensorTypes: SensorType[];
  initialLeaderId: string;
}

export class DistributedSensorNetwork implements NodeNetworkBus {
  private clusters: Map<string, ClusterConfig> = new Map();
  private nodes: Map<string, SensorNode> = new Map();
  private edgeAggregators: Map<string, EdgeAggregator> = new Map();
  private mutexCoordinators: Map<string, MutualExclusionCoordinator> = new Map();
  private grpcClient: AggregationGrpcClient;
  private wsClient: WebSocket | null = null;
  private isRunning = false;

  constructor(grpcServer = 'localhost:9090') {
    this.grpcClient = new AggregationGrpcClient(grpcServer);

    // Define 3 clusters with 4 nodes each = 12 total nodes
    const configs: ClusterConfig[] = [
      {
        clusterId: 'cluster-1',
        clusterName: 'Alpha - Forest Canopy',
        nodeIds: ['S1', 'S2', 'S3', 'S4'],
        sensorTypes: ['TEMPERATURE', 'HUMIDITY', 'MOTION', 'MULTISENSOR'],
        initialLeaderId: 'S4'
      },
      {
        clusterId: 'cluster-2',
        clusterName: 'Beta - River Basin',
        nodeIds: ['S5', 'S6', 'S7', 'S8'],
        sensorTypes: ['WATER_QUALITY', 'WATER_QUALITY', 'TEMPERATURE', 'MULTISENSOR'],
        initialLeaderId: 'S8'
      },
      {
        clusterId: 'cluster-3',
        clusterName: 'Gamma - Urban Perimeter',
        nodeIds: ['S9', 'S10', 'S11', 'S12'],
        sensorTypes: ['TEMPERATURE', 'HUMIDITY', 'MOTION', 'MULTISENSOR'],
        initialLeaderId: 'S12'
      }
    ];

    for (const c of configs) {
      this.clusters.set(c.clusterId, c);
      this.mutexCoordinators.set(c.clusterId, new MutualExclusionCoordinator(8000));

      const aggregator = new EdgeAggregator(
        c.clusterId,
        c.initialLeaderId,
        3500, // 3.5s window
        (summary) => this.onSummaryReady(summary)
      );
      this.edgeAggregators.set(c.clusterId, aggregator);

      for (let i = 0; i < c.nodeIds.length; i++) {
        const nodeId = c.nodeIds[i];
        const sensorType = c.sensorTypes[i];
        const node = new SensorNode(
          nodeId,
          c.clusterId,
          sensorType,
          c.nodeIds,
          c.initialLeaderId,
          this
        );
        this.nodes.set(nodeId, node);
      }
    }
  }

  public async start() {
    this.isRunning = true;
    console.log('🚀 Starting Distributed Sensor Network Simulation...');

    // 1. Register clusters with Central Aggregator via gRPC
    for (const [clusterId, config] of this.clusters.entries()) {
      console.log(`📡 Registering ${config.clusterName} (${clusterId}) via gRPC...`);
      await this.grpcClient.registerCluster(
        clusterId,
        config.clusterName,
        config.initialLeaderId,
        config.nodeIds
      );
    }

    // 2. Start edge aggregators
    for (const agg of this.edgeAggregators.values()) {
      agg.start();
    }

    // 3. Start all sensor nodes
    for (const node of this.nodes.values()) {
      node.start();
    }

    // 4. Connect to Spring Boot WebSocket for fault injection controls
    this.connectWebSocket();
    console.log('✅ All 12 distributed nodes running across 3 clusters.');
  }

  public stop() {
    this.isRunning = false;
    for (const node of this.nodes.values()) {
      node.stop();
    }
    for (const agg of this.edgeAggregators.values()) {
      agg.stop();
    }
    if (this.wsClient) {
      this.wsClient.close();
      this.wsClient = null;
    }
    console.log('🛑 Distributed Sensor Network stopped.');
  }

  // --- NodeNetworkBus Implementation ---
  public sendMessage(msg: InterNodeMessage) {
    const target = this.nodes.get(msg.receiverId);
    if (target) {
      target.receiveMessage(msg);
    }
  }

  public broadcast(clusterId: string, msg: InterNodeMessage) {
    const config = this.clusters.get(clusterId);
    if (!config) return;

    for (const memberId of config.nodeIds) {
      if (memberId !== msg.senderId) {
        const target = this.nodes.get(memberId);
        if (target) {
          target.receiveMessage(msg);
        }
      }
    }
  }

  public sendReadingToLeader(clusterId: string, leaderId: string, reading: SensorReading) {
    const aggregator = this.edgeAggregators.get(clusterId);
    if (aggregator) {
      aggregator.ingestReading(reading);
    }
  }

  public reportEventUpstream(
    clusterId: string,
    nodeId: string,
    eventType: string,
    severity: 'INFO' | 'WARNING' | 'CRITICAL',
    description: string,
    vc: VectorClockMap,
    isDeduplicated = false
  ) {
    // If it's an election won event, update aggregator leader pointer
    if (eventType === 'ELECTION_WON') {
      const agg = this.edgeAggregators.get(clusterId);
      if (agg) {
        agg.updateLeader(nodeId);
      }
    }

    this.grpcClient.reportEvent({
      eventId: `EVT-${Date.now()}-${nodeId}`,
      clusterId,
      nodeId,
      eventType,
      severity,
      description,
      timestamp: Date.now(),
      vectorClock: vc,
      isDeduplicated
    });
  }

  private onSummaryReady(summary: ClusterSummaryData) {
    this.grpcClient.submitSummary(summary);
  }

  // --- Fault Injection & Simulation Controls ---
  public failNode(nodeId: string) {
    const node = this.nodes.get(nodeId);
    if (node) {
      node.crash();
      this.grpcClient.reportEvent({
        eventId: `FAULT-${Date.now()}-${nodeId}`,
        clusterId: node.clusterId,
        nodeId,
        eventType: 'NODE_FAILED',
        severity: 'WARNING',
        description: `Controlled fault: Node ${nodeId} killed for election demo`,
        timestamp: Date.now(),
        vectorClock: node.getVectorClockSnapshot()
      });
    }
  }

  public recoverNode(nodeId: string) {
    const node = this.nodes.get(nodeId);
    if (node) {
      node.recover();
      this.grpcClient.reportEvent({
        eventId: `RECOVER-${Date.now()}-${nodeId}`,
        clusterId: node.clusterId,
        nodeId,
        eventType: 'NODE_RECOVERED',
        severity: 'INFO',
        description: `Node ${nodeId} recovered and rejoined cluster`,
        timestamp: Date.now(),
        vectorClock: node.getVectorClockSnapshot()
      });
    }
  }

  public injectDelay(nodeId: string, delayMs: number) {
    const node = this.nodes.get(nodeId);
    if (node) {
      node.setArtificialDelay(delayMs);
      console.log(`⏱️ [Latency Injected] Injected ${delayMs}ms delay on node ${nodeId} to demonstrate vector clock causality.`);
    }
  }

  public triggerConcurrentAlert(clusterId: string, eventType = 'WILDFIRE_ALERT') {
    const config = this.clusters.get(clusterId);
    if (!config) return;

    const mutex = this.mutexCoordinators.get(clusterId);
    if (!mutex) return;

    // Pick 2 sensors in the cluster to concurrently detect the condition
    const candidateSensors = config.nodeIds.slice(0, 2);
    console.log(`🔥 [Simulating Concurrent Alert] Sensors [${candidateSensors.join(', ')}] simultaneously detected ${eventType}! Testing Mutual Exclusion.`);

    for (const sensorId of candidateSensors) {
      const node = this.nodes.get(sensorId);
      if (!node) continue;

      const vc = node.generateAndSendReading(true).vectorClock;
      const result = mutex.acquireAndExecute({
        alertId: `ALT-${Date.now()}-${sensorId}`,
        clusterId,
        sourceSensor: sensorId,
        eventType,
        severity: 'CRITICAL',
        description: `High heat emergency detected (>48°C) by ${sensorId}`,
        timestamp: Date.now(),
        vectorClock: vc
      });

      this.reportEventUpstream(
        clusterId,
        sensorId,
        'ALERT_MUTEX',
        'CRITICAL',
        result.message,
        vc,
        result.isDeduplicated
      );
    }
  }

  public recoverAll() {
    for (const node of this.nodes.values()) {
      if (node.status === 'FAILED') {
        node.recover();
      }
    }
    for (const mutex of this.mutexCoordinators.values()) {
      mutex.reset();
    }
  }

  private connectWebSocket(wsUrl = 'ws://localhost:8080/ws') {
    try {
      this.wsClient = new WebSocket(wsUrl);

      this.wsClient.on('open', () => {
        console.log('🔗 Distributed Network connected to Aggregator WebSocket control channel');
      });

      this.wsClient.on('message', (data: string) => {
        try {
          const packet = JSON.parse(data.toString());
          this.handleSimulationCommand(packet);
        } catch (e) {
          // ignore parsing error
        }
      });

      this.wsClient.on('error', () => {
        // Will retry automatically
      });

      this.wsClient.on('close', () => {
        if (this.isRunning) {
          setTimeout(() => this.connectWebSocket(wsUrl), 3000);
        }
      });
    } catch (e) {
      if (this.isRunning) {
        setTimeout(() => this.connectWebSocket(wsUrl), 3000);
      }
    }
  }

  private handleSimulationCommand(packet: any) {
    if (!packet || !packet.type) return;
    const data = packet.data || {};

    if (packet.type === 'SIMULATION_FAULT') {
      this.failNode(data.nodeId);
    } else if (packet.type === 'SIMULATION_RECOVER') {
      this.recoverNode(data.nodeId);
    } else if (packet.type === 'SIMULATION_TRIGGER_ALERT') {
      this.triggerConcurrentAlert(data.clusterId || 'cluster-1', data.eventType || 'WILDFIRE_ALERT');
    } else if (packet.type === 'SIMULATION_DELAY') {
      this.injectDelay(data.nodeId || 'S1', data.delayMs || 3000);
    } else if (packet.type === 'SIMULATION_RECOVER_ALL') {
      this.recoverAll();
    }
  }

  public getNode(nodeId: string): SensorNode | undefined {
    return this.nodes.get(nodeId);
  }

  public getClusters(): ClusterConfig[] {
    return Array.from(this.clusters.values());
  }
}
