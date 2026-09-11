import { DistributedSensorNetwork } from './clusterNetwork';

const grpcServer = process.env.GRPC_SERVER || 'localhost:9090';

console.log('===============================================================');
console.log('   DISTRIBUTED ENVIRONMENTAL SENSOR NETWORK SIMULATION');
console.log('   3 Clusters x 4 Nodes = 12 Heterogeneous Nodes');
console.log('   Bully Election | Vector Clocks | Heartbeats | Edge Aggregation');
console.log('===============================================================');

const network = new DistributedSensorNetwork(grpcServer);

// Give the Spring Boot gRPC server a moment if launched simultaneously
setTimeout(() => {
  network.start().catch((err) => {
    console.error('Fatal error starting sensor network:', err);
  });
}, 1500);

process.on('SIGINT', () => {
  console.log('\nGracefully shutting down sensor network...');
  network.stop();
  process.exit(0);
});

process.on('SIGTERM', () => {
  console.log('\nGracefully shutting down sensor network...');
  network.stop();
  process.exit(0);
});
