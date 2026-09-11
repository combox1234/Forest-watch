import * as grpc from '@grpc/grpc-js';
import * as protoLoader from '@grpc/proto-loader';
import * as path from 'path';
import { ClusterSummaryData, ClusterEventData } from './types';

export class AggregationGrpcClient {
  private client: any;
  private isConnected = false;

  constructor(serverAddress = 'localhost:9090') {
    const protoPath = path.resolve(__dirname, '../../proto/aggregation.proto');
    
    const packageDefinition = protoLoader.loadSync(protoPath, {
      keepCase: true,
      longs: String,
      enums: String,
      defaults: true,
      oneofs: true
    });

    const protoDescriptor = grpc.loadPackageDefinition(packageDefinition) as any;
    const AggregationService = protoDescriptor.ds.sensor.network.AggregationService;

    this.client = new AggregationService(
      serverAddress,
      grpc.credentials.createInsecure()
    );
  }

  public registerCluster(
    clusterId: string,
    clusterName: string,
    leaderId: string,
    memberNodeIds: string[]
  ): Promise<any> {
    return new Promise((resolve, reject) => {
      this.client.RegisterCluster(
        {
          cluster_id: clusterId,
          cluster_name: clusterName,
          leader_id: leaderId,
          member_node_ids: memberNodeIds,
          registration_time: Date.now()
        },
        (err: any, response: any) => {
          if (err) {
            console.warn(`[gRPC] RegisterCluster error: ${err.message}`);
            return resolve({ success: false, message: err.message });
          }
          resolve(response);
        }
      );
    });
  }

  public submitSummary(summary: ClusterSummaryData): Promise<any> {
    return new Promise((resolve) => {
      this.client.SubmitClusterSummary(
        {
          summary_id: summary.summaryId,
          cluster_id: summary.clusterId,
          leader_id: summary.leaderId,
          window_start_time: summary.windowStartTime,
          window_end_time: summary.windowEndTime,
          sample_count: summary.sampleCount,
          avg_temperature: summary.avgTemperature,
          max_temperature: summary.maxTemperature,
          min_temperature: summary.minTemperature,
          avg_humidity: summary.avgHumidity,
          avg_water_ph: summary.avgWaterPh,
          avg_water_turbidity: summary.avgWaterTurbidity,
          motion_event_count: summary.motionEventCount,
          vector_clock: summary.vectorClock
        },
        (err: any, response: any) => {
          if (err) {
            console.warn(`[gRPC] SubmitClusterSummary error: ${err.message}`);
            return resolve({ success: false, message: err.message });
          }
          resolve(response);
        }
      );
    });
  }

  public reportEvent(event: ClusterEventData): Promise<any> {
    return new Promise((resolve) => {
      this.client.ReportClusterEvent(
        {
          event_id: event.eventId,
          cluster_id: event.clusterId,
          node_id: event.nodeId,
          event_type: event.eventType,
          severity: event.severity,
          description: event.description,
          timestamp: event.timestamp,
          vector_clock: event.vectorClock,
          is_deduplicated: event.isDeduplicated || false
        },
        (err: any, response: any) => {
          if (err) {
            console.warn(`[gRPC] ReportClusterEvent error: ${err.message}`);
            return resolve({ success: false, message: err.message });
          }
          resolve(response);
        }
      );
    });
  }
}
