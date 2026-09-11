import { SensorReading, ClusterSummaryData, VectorClockMap } from './types';
import { randomUUID } from 'crypto';

export type SummaryCallback = (summary: ClusterSummaryData) => void;

export class EdgeAggregator {
  private clusterId: string;
  private leaderId: string;
  private buffer: SensorReading[] = [];
  private windowStartTime: number;
  private windowDurationMs: number;
  private summaryCallback: SummaryCallback;
  private flushTimer: NodeJS.Timeout | null = null;

  constructor(
    clusterId: string,
    leaderId: string,
    windowDurationMs = 4000,
    summaryCallback: SummaryCallback
  ) {
    this.clusterId = clusterId;
    this.leaderId = leaderId;
    this.windowDurationMs = windowDurationMs;
    this.windowStartTime = Date.now();
    this.summaryCallback = summaryCallback;
  }

  public updateLeader(newLeaderId: string) {
    this.leaderId = newLeaderId;
  }

  public start() {
    if (this.flushTimer) clearInterval(this.flushTimer);
    this.windowStartTime = Date.now();
    this.flushTimer = setInterval(() => this.flushSummary(), this.windowDurationMs);
  }

  public stop() {
    if (this.flushTimer) {
      clearInterval(this.flushTimer);
      this.flushTimer = null;
    }
  }

  public ingestReading(reading: SensorReading) {
    this.buffer.push(reading);
  }

  public flushSummary() {
    if (this.buffer.length === 0) {
      this.windowStartTime = Date.now();
      return;
    }

    const readings = [...this.buffer];
    this.buffer = [];
    const windowEndTime = Date.now();

    let tempSum = 0;
    let tempCount = 0;
    let maxTemp = -Infinity;
    let minTemp = Infinity;

    let humSum = 0;
    let humCount = 0;

    let phSum = 0;
    let phCount = 0;

    let turbSum = 0;
    let turbCount = 0;

    let motionCount = 0;

    const mergedClock: VectorClockMap = {};

    for (const r of readings) {
      if (r.temperature !== undefined) {
        tempSum += r.temperature;
        tempCount++;
        if (r.temperature > maxTemp) maxTemp = r.temperature;
        if (r.temperature < minTemp) minTemp = r.temperature;
      }
      if (r.humidity !== undefined) {
        humSum += r.humidity;
        humCount++;
      }
      if (r.waterPh !== undefined) {
        phSum += r.waterPh;
        phCount++;
      }
      if (r.waterTurbidity !== undefined) {
        turbSum += r.waterTurbidity;
        turbCount++;
      }
      if (r.motionDetected) {
        motionCount++;
      }
      // Merge vector clock
      for (const [node, tick] of Object.entries(r.vectorClock || {})) {
        mergedClock[node] = Math.max(mergedClock[node] || 0, tick);
      }
    }

    const summary: ClusterSummaryData = {
      summaryId: randomUUID(),
      clusterId: this.clusterId,
      leaderId: this.leaderId,
      windowStartTime: this.windowStartTime,
      windowEndTime: windowEndTime,
      sampleCount: readings.length,
      avgTemperature: tempCount > 0 ? Number((tempSum / tempCount).toFixed(2)) : 24.5,
      maxTemperature: maxTemp !== -Infinity ? Number(maxTemp.toFixed(2)) : 26.0,
      minTemperature: minTemp !== Infinity ? Number(minTemp.toFixed(2)) : 22.0,
      avgHumidity: humCount > 0 ? Number((humSum / humCount).toFixed(2)) : 65.0,
      avgWaterPh: phCount > 0 ? Number((phSum / phCount).toFixed(2)) : 7.2,
      avgWaterTurbidity: turbCount > 0 ? Number((turbSum / turbCount).toFixed(2)) : 3.5,
      motionEventCount: motionCount,
      vectorClock: mergedClock
    };

    this.windowStartTime = windowEndTime;
    this.summaryCallback(summary);
  }
}
