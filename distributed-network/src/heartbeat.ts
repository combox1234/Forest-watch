import { NodeStatus } from './types';

export interface HeartbeatListener {
  onStatusChange(nodeId: string, oldStatus: NodeStatus, newStatus: NodeStatus): void;
}

export class HeartbeatManager {
  private nodeId: string;
  private peerTimestamps: Map<string, number> = new Map();
  private peerStatuses: Map<string, NodeStatus> = new Map();
  private suspectedTimeoutMs: number;
  private failedTimeoutMs: number;
  private listeners: HeartbeatListener[] = [];
  private checkTimer: NodeJS.Timeout | null = null;

  constructor(nodeId: string, suspectedTimeoutMs = 3000, failedTimeoutMs = 6000) {
    this.nodeId = nodeId;
    this.suspectedTimeoutMs = suspectedTimeoutMs;
    this.failedTimeoutMs = failedTimeoutMs;
  }

  public registerPeer(peerId: string) {
    if (peerId === this.nodeId) return;
    this.peerTimestamps.set(peerId, Date.now());
    this.peerStatuses.set(peerId, 'HEALTHY');
  }

  public recordHeartbeat(peerId: string) {
    const now = Date.now();
    this.peerTimestamps.set(peerId, now);
    const prevStatus = this.peerStatuses.get(peerId) || 'HEALTHY';
    if (prevStatus !== 'HEALTHY') {
      this.peerStatuses.set(peerId, 'HEALTHY');
      this.notifyListeners(peerId, prevStatus, 'HEALTHY');
    }
  }

  public markExplicitFailure(peerId: string) {
    const prevStatus = this.peerStatuses.get(peerId) || 'HEALTHY';
    this.peerStatuses.set(peerId, 'FAILED');
    this.peerTimestamps.set(peerId, 0);
    this.notifyListeners(peerId, prevStatus, 'FAILED');
  }

  public markExplicitRecovery(peerId: string) {
    const prevStatus = this.peerStatuses.get(peerId) || 'FAILED';
    this.peerStatuses.set(peerId, 'HEALTHY');
    this.peerTimestamps.set(peerId, Date.now());
    this.notifyListeners(peerId, prevStatus, 'HEALTHY');
  }

  public getStatus(peerId: string): NodeStatus {
    return this.peerStatuses.get(peerId) || 'HEALTHY';
  }

  public startChecking(intervalMs = 1000) {
    if (this.checkTimer) clearInterval(this.checkTimer);
    this.checkTimer = setInterval(() => this.checkLiveness(), intervalMs);
  }

  public stopChecking() {
    if (this.checkTimer) {
      clearInterval(this.checkTimer);
      this.checkTimer = null;
    }
  }

  private checkLiveness() {
    const now = Date.now();
    for (const [peerId, lastSeen] of this.peerTimestamps.entries()) {
      const elapsed = now - lastSeen;
      const currentStatus = this.peerStatuses.get(peerId) || 'HEALTHY';

      if (elapsed > this.failedTimeoutMs && currentStatus !== 'FAILED') {
        this.peerStatuses.set(peerId, 'FAILED');
        this.notifyListeners(peerId, currentStatus, 'FAILED');
      } else if (elapsed > this.suspectedTimeoutMs && currentStatus === 'HEALTHY') {
        this.peerStatuses.set(peerId, 'SUSPECTED');
        this.notifyListeners(peerId, currentStatus, 'SUSPECTED');
      }
    }
  }

  public addListener(listener: HeartbeatListener) {
    this.listeners.push(listener);
  }

  private notifyListeners(nodeId: string, oldStatus: NodeStatus, newStatus: NodeStatus) {
    for (const listener of this.listeners) {
      listener.onStatusChange(nodeId, oldStatus, newStatus);
    }
  }
}
