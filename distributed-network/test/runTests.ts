import { VectorClock } from '../src/vectorClock';
import { MutualExclusionCoordinator } from '../src/mutualExclusion';
import { EdgeAggregator } from '../src/edgeAggregator';
import { BullyElectionManager } from '../src/bullyElection';

async function runAllDistributedSystemTests() {
  console.log('====================================================');
  console.log('🧪 RUNNING DISTRIBUTED SYSTEM ALGORITHM VERIFICATION');
  console.log('====================================================\n');

  let passed = 0;
  let failed = 0;

  function assert(condition: boolean, testName: string) {
    if (condition) {
      console.log(`  ✅ PASS: ${testName}`);
      passed++;
    } else {
      console.error(`  ❌ FAIL: ${testName}`);
      failed++;
    }
  }

  // ----------------------------------------------------
  // TEST 1: Vector Clocks & Causal Ordering (Algorithm 10.2)
  // ----------------------------------------------------
  console.log('[1] Testing Vector Clocks & Causal Ordering:');
  const clockA = new VectorClock('A', ['A', 'B', 'C']);
  const clockB = new VectorClock('B', ['A', 'B', 'C']);

  clockA.tick(); // A: [A:1, B:0, C:0]
  const v1 = clockA.getClock();

  // B receives message from A
  clockB.update(v1); // B: [A:1, B:1, C:0]
  const v2 = clockB.getClock();

  assert(VectorClock.compare(v1, v2) === 'BEFORE', 'v1 causally happened-before v2 (v1 < v2)');
  assert(VectorClock.compare(v2, v1) === 'AFTER', 'v2 causally happened-after v1 (v2 > v1)');

  // Independent event on C
  const clockC = new VectorClock('C', ['A', 'B', 'C']);
  clockC.tick(); // C: [A:0, B:0, C:1]
  const v3 = clockC.getClock();

  assert(VectorClock.compare(v2, v3) === 'CONCURRENT', 'Independent events v2 and v3 are correctly identified as CONCURRENT (v2 || v3)');

  // ----------------------------------------------------
  // TEST 2: Mutual Exclusion Alert Deduplication (Algorithm 10.4)
  // ----------------------------------------------------
  console.log('\n[2] Testing Mutual Exclusion & Deduplication:');
  const mutex = new MutualExclusionCoordinator(5000);

  const firstAlert = mutex.acquireAndExecute({
    alertId: 'ALT-1',
    clusterId: 'cluster-1',
    sourceSensor: 'S1',
    eventType: 'WILDFIRE_TEMP',
    severity: 'CRITICAL',
    description: 'Temp > 48C',
    timestamp: Date.now(),
    vectorClock: { S1: 4, S2: 2 }
  });

  assert(firstAlert.executed === true && firstAlert.isDeduplicated === false, 'First alert acquires mutex lock and executes action');

  const secondAlert = mutex.acquireAndExecute({
    alertId: 'ALT-2',
    clusterId: 'cluster-1',
    sourceSensor: 'S2',
    eventType: 'WILDFIRE_TEMP',
    severity: 'CRITICAL',
    description: 'Temp > 49C concurrent reading',
    timestamp: Date.now() + 100,
    vectorClock: { S1: 4, S2: 3 }
  });

  assert(secondAlert.executed === false && secondAlert.isDeduplicated === true, 'Concurrent second alert for same event is deduplicated by mutex');

  // ----------------------------------------------------
  // TEST 3: Edge Aggregation (Algorithm 10.5)
  // ----------------------------------------------------
  console.log('\n[3] Testing Edge Aggregator Window Calculation:');
  let generatedSummary: any = null;
  const edge = new EdgeAggregator('cluster-1', 'S4', 1000, (s) => {
    generatedSummary = s;
  });

  edge.ingestReading({
    readingId: 'R1',
    sensorId: 'S1',
    clusterId: 'cluster-1',
    timestamp: Date.now(),
    temperature: 20.0,
    humidity: 60.0,
    vectorClock: { S1: 1 }
  });

  edge.ingestReading({
    readingId: 'R2',
    sensorId: 'S2',
    clusterId: 'cluster-1',
    timestamp: Date.now(),
    temperature: 30.0,
    humidity: 80.0,
    vectorClock: { S2: 1 }
  });

  edge.flushSummary();

  assert(generatedSummary !== null, 'Edge summary successfully generated');
  assert(generatedSummary.sampleCount === 2, 'Sample count is 2');
  assert(generatedSummary.avgTemperature === 25.0, 'Average temperature calculated correctly: (20 + 30) / 2 = 25.0');
  assert(generatedSummary.maxTemperature === 30.0, 'Max temperature is 30.0');
  assert(generatedSummary.minTemperature === 20.0, 'Min temperature is 20.0');
  assert(generatedSummary.avgHumidity === 70.0, 'Average humidity calculated correctly: (60 + 80) / 2 = 70.0');

  // ----------------------------------------------------
  // TEST 4: Bully Algorithm Priority & Election Logic (Algorithm 10.1)
  // ----------------------------------------------------
  console.log('\n[4] Testing Bully Election Logic:');
  const dummyCallbacks = {
    sendMessage: () => {},
    broadcastMessage: () => {},
    onElectedAsLeader: () => {},
    onNewLeaderRecognized: () => {}
  };

  const bullyS2 = new BullyElectionManager('S2', 'cluster-1', ['S1', 'S2', 'S3', 'S4'], 'S4', dummyCallbacks);
  assert(bullyS2.comparePriority('S4', 'S2') > 0, 'S4 has higher priority than S2');
  assert(bullyS2.comparePriority('S1', 'S2') < 0, 'S1 has lower priority than S2');

  const higherNodes = bullyS2.getHigherPriorityNodes();
  assert(higherNodes.length === 2 && higherNodes.includes('S3') && higherNodes.includes('S4'), 'Higher nodes correctly identified as [S3, S4]');

  console.log('\n====================================================');
  console.log(`🏁 TEST RESULTS: ${passed} PASSED, ${failed} FAILED`);
  console.log('====================================================\n');

  if (failed > 0) process.exit(1);
}

runAllDistributedSystemTests().catch(console.error);
