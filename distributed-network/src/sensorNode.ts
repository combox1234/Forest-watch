import { SensorType, NodeStatus, SensorReading, InterNodeMessage, VectorClockMap } from './types';
import { VectorClock } from './vectorClock';
import { BullyElectionManager, ElectionCallbacks } from './bullyElection';
import { HeartbeatManager, HeartbeatListener } from './heartbeat';
import { randomUUID } from 'crypto';

export interface NodeNetworkBus {
  sendMessage(msg: InterNodeMessage): void;
  broadcast(clusterId: string, msg: InterNodeMessage): void;
  sendReadingToLeader(clusterId: string, leaderId: string, reading: SensorReading): void;
  reportEventUpstream(clusterId: string, nodeId: string, eventType: string, severity: 'INFO' | 'WARNING' | 'CRITICAL', description: string, vc: VectorClockMap, isDeduplicated?: boolean): void;
}

export class SensorNode implements ElectionCallbacks, HeartbeatListener {
  public readonly nodeId: string;
  public readonly clusterId: string;
  public readonly sensorType: SensorType;
  public batteryScore = 100.0;
  public status: NodeStatus = 'HEALTHY';

  private vectorClock: VectorClock;
  private electionManager: BullyElectionManager;
  private heartbeatManager: HeartbeatManager;
  private networkBus: NodeNetworkBus;

  private readingTimer: NodeJS.Timeout | null = null;
  private heartbeatTimer: NodeJS.Timeout | null = null;
  private isAlive = true;
  private artificialDelayMs = 0;

  constructor(
    nodeId: string,
    clusterId: string,
    sensorType: SensorType,
    allClusterNodes: string[],
    initialLeaderId: string,
    networkBus: NodeNetworkBus
  ) {
    this.nodeId = nodeId;
    this.clusterId = clusterId;
    this.sensorType = sensorType;
    this.networkBus = networkBus;

    this.vectorClock = new VectorClock(nodeId, allClusterNodes);
    this.electionManager = new BullyElectionManager(
      nodeId,
      clusterId,
      allClusterNodes,
      initialLeaderId,
      this
    );

    this.heartbeatManager = new HeartbeatManager(nodeId, 3200, 6000);
    this.heartbeatManager.addListener(this);

    for (const peerId of allClusterNodes) {
      if (peerId !== nodeId) {
        this.heartbeatManager.registerPeer(peerId);
      }
    }
  }

  public start() {
    this.isAlive = true;
    this.status = 'HEALTHY';
    this.heartbeatManager.startChecking(1000);

    // Periodic Heartbeats (every 1.5s)
    this.heartbeatTimer = setInterval(() => {
      if (!this.isAlive) return;
      this.sendHeartbeat();
    }, 1500);

    // Periodic Sensor Readings (every 2.5s)
    this.readingTimer = setInterval(() => {
      if (!this.isAlive) return;
      this.generateAndSendReading();
    }, 2500);
  }

  public stop() {
    this.isAlive = false;
    this.status = 'FAILED';
    if (this.readingTimer) clearInterval(this.readingTimer);
    if (this.heartbeatTimer) clearInterval(this.heartbeatTimer);
    this.heartbeatManager.stopChecking();
  }

  public setArtificialDelay(delayMs: number) {
    this.artificialDelayMs = delayMs;
  }

  /**
   * Fault injection: Crash this node
   */
  public crash() {
    console.log(`💥 [Node Crash] Node ${this.nodeId} in ${this.clusterId} has CRASHED.`);
    this.stop();
  }

  /**
   * Fault injection: Recover this node
   */
  public recover() {
    console.log(`✨ [Node Recover] Node ${this.nodeId} in ${this.clusterId} is RECOVERING.`);
    this.start();
    // Start an election if we are higher priority than the current leader
    const currentLeader = this.electionManager.getCurrentLeader();
    if (currentLeader && this.electionManager.comparePriority(this.nodeId, currentLeader) > 0) {
      console.log(`[Bully Election] Node ${this.nodeId} recovered with higher priority than current leader ${currentLeader}. Starting re-election.`);
      this.electionManager.startElection(`Higher priority node ${this.nodeId} recovered`);
    }
  }

  /**
   * Generate an environmental reading
   */
  public generateAndSendReading(forceEmergency = false): SensorReading {
    this.vectorClock.tick();

    // Natural slight battery drain
    this.batteryScore = Math.max(10, Number((this.batteryScore - 0.02).toFixed(1)));

    let temp = 22.0 + Math.random() * 8.0;
    let hum = 50.0 + Math.random() * 25.0;
    let ph = 6.8 + Math.random() * 1.2;
    let turb = 2.0 + Math.random() * 4.0;
    let motion = Math.random() > 0.85;

    if (forceEmergency) {
      temp = 48.5; // High wildfire temp
      motion = true;
    }

    const reading: SensorReading = {
      readingId: randomUUID(),
      sensorId: this.nodeId,
      clusterId: this.clusterId,
      timestamp: Date.now(),
      temperature: Number(temp.toFixed(2)),
      humidity: Number(hum.toFixed(2)),
      waterPh: Number(ph.toFixed(2)),
      waterTurbidity: Number(turb.toFixed(2)),
      motionDetected: motion,
      vectorClock: this.vectorClock.getClock()
    };

    const leaderId = this.electionManager.getCurrentLeader();
    if (leaderId) {
      this.dispatchWithDelay(() => {
        this.networkBus.sendReadingToLeader(this.clusterId, leaderId, reading);
      });
    }

    return reading;
  }

  private sendHeartbeat() {
    const leaderId = this.electionManager.getCurrentLeader();
    const msg: InterNodeMessage = {
      messageId: randomUUID(),
      senderId: this.nodeId,
      receiverId: leaderId || 'BROADCAST',
      clusterId: this.clusterId,
      type: 'HEARTBEAT',
      vectorClock: this.vectorClock.getClock(),
      timestamp: Date.now(),
      payload: { status: this.status, battery: this.batteryScore }
    };

    this.dispatchWithDelay(() => {
      this.networkBus.broadcast(this.clusterId, msg);
    });
  }

  public receiveMessage(msg: InterNodeMessage) {
    if (!this.isAlive) return; // Dead node ignores messages

    // Update vector clock with received clock
    this.vectorClock.update(msg.vectorClock);

    // Record heartbeat for peer
    if (msg.type === 'HEARTBEAT') {
      this.heartbeatManager.recordHeartbeat(msg.senderId);
      return;
    }

    // Handle Bully Election messages
    if (msg.type === 'ELECTION') {
      this.electionManager.handleElectionMessage(msg.senderId);
    } else if (msg.type === 'OK') {
      this.electionManager.handleOkMessage(msg.senderId);
    } else if (msg.type === 'COORDINATOR') {
      this.electionManager.handleCoordinatorMessage(msg.payload?.leaderId || msg.senderId);
    }
  }

  // --- HeartbeatListener Implementation ---
  public onStatusChange(peerId: string, oldStatus: NodeStatus, newStatus: NodeStatus) {
    console.log(`[Heartbeat Monitor] [${this.nodeId}]: Peer ${peerId} changed status: ${oldStatus} -> ${newStatus}`);

    const currentLeader = this.electionManager.getCurrentLeader();
    // If the peer that failed or is suspected was our cluster leader, TRIGGER BULLY ELECTION!
    if (peerId === currentLeader && (newStatus === 'SUSPECTED' || newStatus === 'FAILED')) {
      console.log(`🚨 [Leader Failure Detected] Node ${this.nodeId} detected leader ${currentLeader} is ${newStatus}! Initiating Bully Election.`);
      this.electionManager.startElection(`Leader ${currentLeader} heartbeat ${newStatus}`);
    }
  }

  // --- ElectionCallbacks Implementation ---
  public sendMessage(targetId: string, type: 'ELECTION' | 'OK' | 'COORDINATOR', payload?: any) {
    this.vectorClock.tick();
    const msg: InterNodeMessage = {
      messageId: randomUUID(),
      senderId: this.nodeId,
      receiverId: targetId,
      clusterId: this.clusterId,
      type,
      vectorClock: this.vectorClock.getClock(),
      timestamp: Date.now(),
      payload
    };
    this.dispatchWithDelay(() => {
      this.networkBus.sendMessage(msg);
    });
  }

  public broadcastMessage(type: 'ELECTION' | 'OK' | 'COORDINATOR', payload?: any) {
    this.vectorClock.tick();
    const msg: InterNodeMessage = {
      messageId: randomUUID(),
      senderId: this.nodeId,
      receiverId: 'BROADCAST',
      clusterId: this.clusterId,
      type,
      vectorClock: this.vectorClock.getClock(),
      timestamp: Date.now(),
      payload
    };
    this.dispatchWithDelay(() => {
      this.networkBus.broadcast(this.clusterId, msg);
    });
  }

  public onElectedAsLeader(durationMs: number, reason: string) {
    console.log(`🎉 [New Leader] ${this.nodeId} is now the leader of ${this.clusterId}. Reporting to Aggregator.`);
    this.networkBus.reportEventUpstream(
      this.clusterId,
      this.nodeId,
      'ELECTION_WON',
      'INFO',
      `Bully Election won by ${this.nodeId} (${reason}) in ${durationMs}ms`,
      this.vectorClock.getClock()
    );
  }

  public onNewLeaderRecognized(newLeaderId: string) {
    console.log(`[Leader Update] ${this.nodeId} updated leader to ${newLeaderId}`);
  }

  private dispatchWithDelay(fn: () => void) {
    if (this.artificialDelayMs > 0) {
      const delay = this.artificialDelayMs;
      this.artificialDelayMs = 0; // one-shot delay for causal demonstration
      setTimeout(fn, delay);
    } else {
      fn();
    }
  }

  public isCoordinator(): boolean {
    return this.electionManager.isLeader();
  }

  public getVectorClockSnapshot(): VectorClockMap {
    return this.vectorClock.getClock();
  }

  public getLeader(): string | null {
    return this.electionManager.getCurrentLeader();
  }
}
