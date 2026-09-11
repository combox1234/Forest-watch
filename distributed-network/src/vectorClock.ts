import { VectorClockMap } from './types';

export class VectorClock {
  private nodeId: string;
  private clock: VectorClockMap = {};

  constructor(nodeId: string, knownNodes: string[] = []) {
    this.nodeId = nodeId;
    for (const node of knownNodes) {
      this.clock[node] = 0;
    }
    this.clock[nodeId] = 0;
  }

  /**
   * Local event: increment local component V[i] = V[i] + 1
   */
  public tick(): VectorClockMap {
    this.clock[this.nodeId] = (this.clock[this.nodeId] || 0) + 1;
    return this.getClock();
  }

  /**
   * Message receive: component-wise max, then local increment
   * V[k] = max(V[k], M[k]) for every k; then V[i] = V[i] + 1
   */
  public update(receivedClock: VectorClockMap): VectorClockMap {
    for (const [node, time] of Object.entries(receivedClock)) {
      this.clock[node] = Math.max(this.clock[node] || 0, time);
    }
    this.clock[this.nodeId] = (this.clock[this.nodeId] || 0) + 1;
    return this.getClock();
  }

  /**
   * Get an immutable snapshot of current clock
   */
  public getClock(): VectorClockMap {
    return { ...this.clock };
  }

  /**
   * Static comparison between two vector clocks:
   * Returns:
   *  'BEFORE' if v1 < v2 (v1 causally preceded v2)
   *  'AFTER' if v2 < v1 (v1 causally succeeded v2)
   *  'EQUAL' if v1 == v2
   *  'CONCURRENT' if v1 || v2 (cannot be causally ordered)
   */
  public static compare(v1: VectorClockMap, v2: VectorClockMap): 'BEFORE' | 'AFTER' | 'EQUAL' | 'CONCURRENT' {
    const allKeys = Array.from(new Set([...Object.keys(v1), ...Object.keys(v2)]));
    let strictlyLess = false;
    let strictlyGreater = false;

    for (const key of allKeys) {
      const val1 = v1[key] || 0;
      const val2 = v2[key] || 0;

      if (val1 < val2) {
        strictlyLess = true;
      } else if (val1 > val2) {
        strictlyGreater = true;
      }
    }

    if (strictlyLess && !strictlyGreater) return 'BEFORE';
    if (strictlyGreater && !strictlyLess) return 'AFTER';
    if (!strictlyLess && !strictlyGreater) return 'EQUAL';
    return 'CONCURRENT';
  }

  /**
   * Format clock for human readable display (e.g. "[S1:3, S2:5]")
   */
  public static format(vc: VectorClockMap): string {
    return '[' + Object.entries(vc)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([k, v]) => `${k}:${v}`)
      .join(', ') + ']';
  }
}
