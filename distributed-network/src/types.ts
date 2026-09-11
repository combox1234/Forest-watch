export type SensorType = 
  | 'TEMPERATURE' 
  | 'HUMIDITY' 
  | 'WATER_QUALITY' 
  | 'MOTION' 
  | 'MULTISENSOR';

export type NodeStatus = 'HEALTHY' | 'SUSPECTED' | 'FAILED';

export type VectorClockMap = Record<string, number>;

export type MessageType = 
  | 'HEARTBEAT'
  | 'HEARTBEAT_ACK'
  | 'ELECTION'
  | 'OK'
  | 'COORDINATOR'
  | 'READING_DATA'
  | 'MUTEX_REQUEST'
  | 'MUTEX_REPLY'
  | 'ALERT_TRIGGER';

export interface InterNodeMessage {
  messageId: string;
  senderId: string;
  receiverId: string; // or 'BROADCAST'
  clusterId: string;
  type: MessageType;
  vectorClock: VectorClockMap;
  timestamp: number;
  payload?: any;
}

export interface SensorReading {
  readingId: string;
  sensorId: string;
  clusterId: string;
  timestamp: number;
  temperature?: number;
  humidity?: number;
  waterPh?: number;
  waterTurbidity?: number;
  motionDetected?: boolean;
  vectorClock: VectorClockMap;
}

export interface ClusterSummaryData {
  summaryId: string;
  clusterId: string;
  leaderId: string;
  windowStartTime: number;
  windowEndTime: number;
  sampleCount: number;
  avgTemperature: number;
  maxTemperature: number;
  minTemperature: number;
  avgHumidity: number;
  avgWaterPh: number;
  avgWaterTurbidity: number;
  motionEventCount: number;
  vectorClock: VectorClockMap;
}

export interface ClusterEventData {
  eventId: string;
  clusterId: string;
  nodeId: string;
  eventType: string; // ELECTION, ALERT_MUTEX, HEARTBEAT_TIMEOUT, NODE_FAILED, NODE_RECOVERED
  severity: 'INFO' | 'WARNING' | 'CRITICAL';
  description: string;
  timestamp: number;
  vectorClock: VectorClockMap;
  isDeduplicated?: boolean;
}
