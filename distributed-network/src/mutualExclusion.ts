export interface AlertExecutionContext {
  alertId: string;
  clusterId: string;
  sourceSensor: string;
  eventType: string;
  severity: 'INFO' | 'WARNING' | 'CRITICAL';
  description: string;
  timestamp: number;
  vectorClock: Record<string, number>;
}

export interface MutexResult {
  executed: boolean;
  isDeduplicated: boolean;
  lockHolder: string;
  message: string;
}

export class MutualExclusionCoordinator {
  private activeAlertLocks: Map<string, { winnerSensor: string; lockTime: number }> = new Map();
  private deduplicationWindowMs: number;

  constructor(deduplicationWindowMs = 8000) {
    this.deduplicationWindowMs = deduplicationWindowMs;
  }

  /**
   * Attempt to execute a shared critical alert action.
   * If an alert of this type in this cluster was already executed within the deduplication window,
   * the duplicate is rejected and marked deduplicated.
   */
  public acquireAndExecute(context: AlertExecutionContext): MutexResult {
    const lockKey = `${context.clusterId}:${context.eventType}`;
    const now = Date.now();
    const existingLock = this.activeAlertLocks.get(lockKey);

    if (existingLock && (now - existingLock.lockTime) < this.deduplicationWindowMs) {
      // Duplicate alert detected! Mutual exclusion prevents double dispatch.
      console.log(`🛡️ [Mutual Exclusion] Deduplicated concurrent alert [${context.eventType}] from [${context.sourceSensor}]. Winner was [${existingLock.winnerSensor}].`);
      return {
        executed: false,
        isDeduplicated: true,
        lockHolder: existingLock.winnerSensor,
        message: `Mutual exclusion: Duplicate ${context.eventType} suppressed (Action already executed by ${existingLock.winnerSensor})`
      };
    }

    // Acquire lock and execute action
    this.activeAlertLocks.set(lockKey, {
      winnerSensor: context.sourceSensor,
      lockTime: now
    });

    console.log(`⚡ [Mutual Exclusion] Lock granted to [${context.sourceSensor}] for [${context.eventType}] in [${context.clusterId}]. Executing shared alert action!`);
    return {
      executed: true,
      isDeduplicated: false,
      lockHolder: context.sourceSensor,
      message: `Mutual exclusion: Lock granted to ${context.sourceSensor}. Action executed.`
    };
  }

  /**
   * Reset locks (for demo reset)
   */
  public reset() {
    this.activeAlertLocks.clear();
  }
}
