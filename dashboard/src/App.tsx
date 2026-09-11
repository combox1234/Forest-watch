import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Header } from './components/Header';
import { SystemOverview } from './components/SystemOverview';
import { ClusterTopology } from './components/ClusterTopology';
import { BullyElectionPanel } from './components/BullyElectionPanel';
import { VectorClockTimeline } from './components/VectorClockTimeline';
import { MutualExclusionPanel } from './components/MutualExclusionPanel';
import { EdgeAggregationChart } from './components/EdgeAggregationChart';
import { FaultInjectionConsole } from './components/FaultInjectionConsole';
import { SensorNode, ClusterGroup, ReadingSummary, AlertEvent, ElectionRecord, SystemStats } from './types';

export const App: React.FC = () => {
  const [nodes, setNodes] = useState<SensorNode[]>([]);
  const [clusters, setClusters] = useState<ClusterGroup[]>([]);
  const [summaries, setSummaries] = useState<ReadingSummary[]>([]);
  const [alerts, setAlerts] = useState<AlertEvent[]>([]);
  const [elections, setElections] = useState<ElectionRecord[]>([]);
  const [stats, setStats] = useState<SystemStats>({
    totalNodes: 12,
    healthyNodes: 12,
    failedNodes: 0,
    suspectedNodes: 0,
    totalClusters: 3,
    totalAlerts: 0,
    totalSummaries: 0,
    totalElections: 0
  });
  const [wsConnected, setWsConnected] = useState(false);
  const [latestSummaries, setLatestSummaries] = useState<Record<string, ReadingSummary>>({});

  const wsRef = useRef<WebSocket | null>(null);

  // Fetch initial REST data from Aggregator
  const fetchData = useCallback(async () => {
    try {
      const [nodesRes, clustersRes, readingsRes, alertsRes, electionsRes, statsRes] = await Promise.all([
        fetch('/api/sensors').then(r => r.ok ? r.json() : []),
        fetch('/api/clusters').then(r => r.ok ? r.json() : []),
        fetch('/api/readings').then(r => r.ok ? r.json() : []),
        fetch('/api/alerts').then(r => r.ok ? r.json() : []),
        fetch('/api/elections').then(r => r.ok ? r.json() : []),
        fetch('/api/stats').then(r => r.ok ? r.json() : null)
      ]);

      if (nodesRes && nodesRes.length > 0) setNodes(nodesRes);
      if (clustersRes && clustersRes.length > 0) setClusters(clustersRes);
      if (readingsRes && readingsRes.length > 0) {
        setSummaries(readingsRes);
        const map: Record<string, ReadingSummary> = {};
        for (const s of readingsRes) {
          if (!map[s.clusterId]) map[s.clusterId] = s;
        }
        setLatestSummaries(map);
      }
      if (alertsRes && alertsRes.length > 0) setAlerts(alertsRes);
      if (electionsRes && electionsRes.length > 0) setElections(electionsRes);
      if (statsRes) setStats(statsRes);
    } catch (err) {
      console.warn('API polling fallback notice:', err);
    }
  }, []);

  // WebSocket real-time connection
  useEffect(() => {
    fetchData();

    const connectWs = () => {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const wsHost = window.location.host;
      const wsUrl = `${protocol}//${wsHost}/ws`;

      const ws = new WebSocket(wsUrl);
      wsRef.current = ws;

      ws.onopen = () => {
        console.log('Connected to Aggregator WebSocket');
        setWsConnected(true);
      };

      ws.onmessage = (event) => {
        try {
          const packet = JSON.parse(event.data);
          const type = packet.type;
          const data = packet.data;

          if (type === 'SUMMARY_RECEIVED') {
            setSummaries(prev => [data, ...prev.slice(0, 49)]);
            setLatestSummaries(prev => ({ ...prev, [data.clusterId]: data }));
            setStats(prev => ({ ...prev, totalSummaries: prev.totalSummaries + 1 }));
          } else if (type === 'CLUSTER_REGISTERED') {
            setClusters(prev => {
              const existing = prev.find(c => c.clusterId === data.clusterId);
              if (existing) {
                return prev.map(c => c.clusterId === data.clusterId ? { ...c, leaderId: data.leaderId, status: data.status } : c);
              }
              return [...prev, {
                clusterId: data.clusterId,
                clusterName: data.clusterName,
                leaderId: data.leaderId,
                status: data.status,
                lastElectionTime: Date.now(),
                memberCount: data.members ? data.members.length : 4
              }];
            });
            fetchData();
          } else if (type === 'CLUSTER_EVENT') {
            if (data.eventType === 'ELECTION_WON' || data.eventType === 'ELECTION') {
              setElections(prev => [{
                electionId: data.eventId,
                clusterId: data.clusterId,
                initiator: data.nodeId,
                electedLeader: data.nodeId,
                startedAt: data.timestamp,
                completedAt: data.timestamp,
                reason: data.description,
                durationMs: 250
              }, ...prev]);

              setClusters(prev => prev.map(c =>
                c.clusterId === data.clusterId ? { ...c, leaderId: data.nodeId } : c
              ));
              setStats(prev => ({ ...prev, totalElections: prev.totalElections + 1 }));
            } else if (data.eventType === 'ALERT_MUTEX' || data.eventType === 'ALERT') {
              setAlerts(prev => [{
                alertId: data.eventId,
                clusterId: data.clusterId,
                eventType: data.eventType,
                severity: data.severity,
                sourceSensor: data.nodeId,
                eventTime: data.timestamp,
                status: data.isDeduplicated ? 'DEDUPLICATED_MUTEX' : 'TRIGGERED',
                description: data.description,
                isDeduplicated: data.isDeduplicated || false
              }, ...prev]);
              setStats(prev => ({ ...prev, totalAlerts: prev.totalAlerts + 1 }));
            } else if (data.eventType === 'NODE_FAILED') {
              setNodes(prev => prev.map(n => n.sensorId === data.nodeId ? { ...n, status: 'FAILED' } : n));
            } else if (data.eventType === 'NODE_RECOVERED') {
              setNodes(prev => prev.map(n => n.sensorId === data.nodeId ? { ...n, status: 'HEALTHY' } : n));
            }
          } else if (type === 'SIMULATION_FAULT') {
            setNodes(prev => prev.map(n => n.sensorId === data.nodeId ? { ...n, status: 'FAILED' } : n));
          } else if (type === 'SIMULATION_RECOVER') {
            setNodes(prev => prev.map(n => n.sensorId === data.nodeId ? { ...n, status: 'HEALTHY' } : n));
          } else if (type === 'SIMULATION_RECOVER_ALL') {
            setNodes(prev => prev.map(n => ({ ...n, status: 'HEALTHY' })));
          }
        } catch (e) {
          // ignore parsing error
        }
      };

      ws.onclose = () => {
        setWsConnected(false);
        setTimeout(connectWs, 3000);
      };

      ws.onerror = () => {
        ws.close();
      };
    };

    connectWs();

    // Secondary periodic poll to sync full DB state
    const pollTimer = setInterval(fetchData, 4000);

    return () => {
      clearInterval(pollTimer);
      if (wsRef.current) wsRef.current.close();
    };
  }, [fetchData]);

  // Fault injection handlers
  const handleFailNode = async (nodeId: string) => {
    try {
      await fetch(`/api/simulation/fault/${nodeId}`, { method: 'POST' });
      setNodes(prev => prev.map(n => n.sensorId === nodeId ? { ...n, status: 'FAILED' } : n));
    } catch (e) {
      console.error(e);
    }
  };

  const handleRecoverNode = async (nodeId: string) => {
    try {
      await fetch(`/api/simulation/recover/${nodeId}`, { method: 'POST' });
      setNodes(prev => prev.map(n => n.sensorId === nodeId ? { ...n, status: 'HEALTHY' } : n));
    } catch (e) {
      console.error(e);
    }
  };

  const handleTriggerConcurrentAlert = async (clusterId: string) => {
    try {
      await fetch('/api/simulation/trigger-alert', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ clusterId, eventType: 'WILDFIRE_ALERT' })
      });
    } catch (e) {
      console.error(e);
    }
  };

  const handleInjectDelay = async (nodeId: string, delayMs: number) => {
    try {
      await fetch('/api/simulation/delay', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ nodeId, delayMs })
      });
    } catch (e) {
      console.error(e);
    }
  };

  const handleRecoverAll = async () => {
    try {
      await fetch('/api/simulation/recover-all', { method: 'POST' });
      setNodes(prev => prev.map(n => ({ ...n, status: 'HEALTHY' })));
    } catch (e) {
      console.error(e);
    }
  };

  const handleClearAlerts = () => {
    setAlerts([]);
    setStats(prev => ({ ...prev, totalAlerts: 0 }));
  };

  // Fallback defaults if clusters not registered yet
  const displayClusters: ClusterGroup[] = clusters.length > 0 ? clusters : [
    { clusterId: 'cluster-1', clusterName: 'Alpha - Forest Canopy', leaderId: 'S4', status: 'ACTIVE', lastElectionTime: Date.now(), memberCount: 4 },
    { clusterId: 'cluster-2', clusterName: 'Beta - River Basin', leaderId: 'S8', status: 'ACTIVE', lastElectionTime: Date.now(), memberCount: 4 },
    { clusterId: 'cluster-3', clusterName: 'Gamma - Urban Perimeter', leaderId: 'S12', status: 'ACTIVE', lastElectionTime: Date.now(), memberCount: 4 }
  ];

  // Fallback defaults if nodes not registered yet
  const displayNodes: SensorNode[] = nodes.length > 0 ? nodes : [
    { sensorId: 'S1', clusterId: 'cluster-1', type: 'TEMPERATURE', status: 'HEALTHY', batteryScore: 98, lastHeartbeat: Date.now() },
    { sensorId: 'S2', clusterId: 'cluster-1', type: 'HUMIDITY', status: 'HEALTHY', batteryScore: 95, lastHeartbeat: Date.now() },
    { sensorId: 'S3', clusterId: 'cluster-1', type: 'MOTION', status: 'HEALTHY', batteryScore: 92, lastHeartbeat: Date.now() },
    { sensorId: 'S4', clusterId: 'cluster-1', type: 'MULTISENSOR', status: 'HEALTHY', batteryScore: 99, lastHeartbeat: Date.now() },
    { sensorId: 'S5', clusterId: 'cluster-2', type: 'WATER_QUALITY', status: 'HEALTHY', batteryScore: 97, lastHeartbeat: Date.now() },
    { sensorId: 'S6', clusterId: 'cluster-2', type: 'WATER_QUALITY', status: 'HEALTHY', batteryScore: 96, lastHeartbeat: Date.now() },
    { sensorId: 'S7', clusterId: 'cluster-2', type: 'TEMPERATURE', status: 'HEALTHY', batteryScore: 94, lastHeartbeat: Date.now() },
    { sensorId: 'S8', clusterId: 'cluster-2', type: 'MULTISENSOR', status: 'HEALTHY', batteryScore: 99, lastHeartbeat: Date.now() },
    { sensorId: 'S9', clusterId: 'cluster-3', type: 'TEMPERATURE', status: 'HEALTHY', batteryScore: 96, lastHeartbeat: Date.now() },
    { sensorId: 'S10', clusterId: 'cluster-3', type: 'HUMIDITY', status: 'HEALTHY', batteryScore: 93, lastHeartbeat: Date.now() },
    { sensorId: 'S11', clusterId: 'cluster-3', type: 'MOTION', status: 'HEALTHY', batteryScore: 91, lastHeartbeat: Date.now() },
    { sensorId: 'S12', clusterId: 'cluster-3', type: 'MULTISENSOR', status: 'HEALTHY', batteryScore: 100, lastHeartbeat: Date.now() }
  ];

  const activeNodesCount = displayNodes.filter(n => n.status === 'HEALTHY').length;
  const activeLeadersCount = displayClusters.filter(c => !!c.leaderId).length;

  return (
    <div className="container">
      {/* Header */}
      <Header
        wsConnected={wsConnected}
        activeNodes={activeNodesCount}
        totalNodes={displayNodes.length}
        totalClusters={displayClusters.length}
      />

      {/* System Overview KPIs */}
      <SystemOverview
        stats={{
          ...stats,
          totalNodes: displayNodes.length,
          healthyNodes: activeNodesCount,
          failedNodes: displayNodes.length - activeNodesCount,
          totalClusters: displayClusters.length
        }}
        totalClusters={displayClusters.length}
        activeLeaders={activeLeadersCount}
      />

      {/* Fault Injection Console */}
      <FaultInjectionConsole
        clusters={displayClusters}
        onFailNode={handleFailNode}
        onTriggerConcurrentAlert={handleTriggerConcurrentAlert}
        onInjectDelay={handleInjectDelay}
        onRecoverAll={handleRecoverAll}
      />

      {/* Cluster Topology Cards */}
      <ClusterTopology
        clusters={displayClusters}
        nodes={displayNodes}
        latestSummaries={latestSummaries}
        onFailNode={handleFailNode}
        onRecoverNode={handleRecoverNode}
      />

      {/* Edge Aggregation & Throughput Savings */}
      <EdgeAggregationChart summaries={summaries} />

      {/* Bully Election & Mutual Exclusion — full width, stacked */}
      <BullyElectionPanel elections={elections} />
      <MutualExclusionPanel alerts={alerts} onClearAlerts={handleClearAlerts} />

      {/* Vector Clock & Causal Timeline */}
      <VectorClockTimeline summaries={summaries} />

      {/* Footer */}
      <footer style={{ textAlign: 'center', padding: '2rem 0', color: 'var(--text-dim)', fontSize: '0.8rem', borderTop: '1px solid var(--border-card)', marginTop: '0.5rem' }}>
        Distributed Systems Final Year B.Tech Mini Project &nbsp;·&nbsp; Curriculum Regulations 2023 (w.e.f. 2026–27)
        <div style={{ marginTop: '4px', color: 'var(--text-muted)' }}>
          Spring Boot Aggregator (REST :8080, gRPC :9090) &nbsp;·&nbsp; Dynamic Cluster Heads &nbsp;·&nbsp; Bully Election &nbsp;·&nbsp; Vector Clocks
        </div>
      </footer>
    </div>
  );
};

export default App;
