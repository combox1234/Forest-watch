export interface SensorNode {
  sensorId: string;
  clusterId: string;
  type: string;
  status: 'HEALTHY' | 'SUSPECTED' | 'FAILED';
  batteryScore: number;
  lastHeartbeat: number;
  vectorClockJson?: string;
}

export interface ClusterGroup {
  clusterId: string;
  clusterName: string;
  leaderId: string;
  status: string;
  lastElectionTime: number;
  memberCount: number;
}

export interface ReadingSummary {
  readingId: string;
  clusterId: string;
  leaderId: string;
  timestamp: number;
  sampleCount: number;
  avgTemperature: number;
  maxTemperature: number;
  minTemperature: number;
  avgHumidity: number;
  avgWaterPh: number;
  avgWaterTurbidity: number;
  motionEventCount: number;
  vectorClockJson?: string;
  vectorClock?: Record<string, number>;
}

export interface AlertEvent {
  alertId: string;
  clusterId: string;
  eventType: string;
  severity: 'INFO' | 'WARNING' | 'CRITICAL';
  sourceSensor: string;
  eventTime: number;
  status: string;
  description: string;
  isDeduplicated: boolean;
  vectorClockJson?: string;
  vectorClock?: Record<string, number>;
}

export interface ElectionRecord {
  electionId: string;
  clusterId: string;
  initiator: string;
  electedLeader: string;
  startedAt: number;
  completedAt: number;
  reason: string;
  durationMs: number;
}

export interface SystemStats {
  totalNodes: number;
  healthyNodes: number;
  failedNodes: number;
  suspectedNodes: number;
  totalClusters: number;
  totalAlerts: number;
  totalSummaries: number;
  totalElections: number;
}
