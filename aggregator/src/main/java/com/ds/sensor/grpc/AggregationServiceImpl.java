package com.ds.sensor.grpc;

import com.ds.sensor.model.*;
import com.ds.sensor.repository.*;
import com.ds.sensor.websocket.DashboardWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AggregationServiceImpl extends AggregationServiceGrpc.AggregationServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(AggregationServiceImpl.class);

    private final ClusterGroupRepository clusterRepository;
    private final SensorNodeRepository sensorRepository;
    private final ReadingRecordRepository readingRepository;
    private final AlertRecordRepository alertRepository;
    private final ElectionRecordRepository electionRepository;
    private final HeartbeatRecordRepository heartbeatRepository;
    private final DashboardWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AggregationServiceImpl(
            ClusterGroupRepository clusterRepository,
            SensorNodeRepository sensorRepository,
            ReadingRecordRepository readingRepository,
            AlertRecordRepository alertRepository,
            ElectionRecordRepository electionRepository,
            HeartbeatRecordRepository heartbeatRepository,
            DashboardWebSocketHandler webSocketHandler) {
        this.clusterRepository = clusterRepository;
        this.sensorRepository = sensorRepository;
        this.readingRepository = readingRepository;
        this.alertRepository = alertRepository;
        this.electionRepository = electionRepository;
        this.heartbeatRepository = heartbeatRepository;
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void registerCluster(ClusterRegistration request, StreamObserver<Ack> responseObserver) {
        try {
            log.info("Registering cluster: {} ({}), Leader: {}, Members: {}",
                    request.getClusterId(), request.getClusterName(), request.getLeaderId(), request.getMemberNodeIdsList());

            ClusterGroupEntity cluster = clusterRepository.findById(request.getClusterId())
                    .orElse(new ClusterGroupEntity());
            cluster.setClusterId(request.getClusterId());
            cluster.setClusterName(request.getClusterName());
            cluster.setLeaderId(request.getLeaderId());
            cluster.setStatus("ACTIVE");
            cluster.setMemberCount(request.getMemberNodeIdsCount());
            cluster.setLastElectionTime(request.getRegistrationTime() > 0 ? request.getRegistrationTime() : System.currentTimeMillis());
            clusterRepository.save(cluster);

            // Register or update members
            for (String nodeId : request.getMemberNodeIdsList()) {
                SensorNodeEntity sensor = sensorRepository.findById(nodeId)
                        .orElse(new SensorNodeEntity(nodeId, request.getClusterId(), "MULTISENSOR", "HEALTHY", 100.0));
                sensor.setClusterId(request.getClusterId());
                sensor.setStatus("HEALTHY");
                sensor.setLastHeartbeat(System.currentTimeMillis());
                sensorRepository.save(sensor);
            }

            webSocketHandler.broadcast("CLUSTER_REGISTERED", Map.of(
                    "clusterId", request.getClusterId(),
                    "clusterName", request.getClusterName(),
                    "leaderId", request.getLeaderId(),
                    "members", request.getMemberNodeIdsList(),
                    "status", "ACTIVE"
            ));

            Ack ack = Ack.newBuilder()
                    .setSuccess(true)
                    .setMessage("Cluster " + request.getClusterId() + " successfully registered with leader " + request.getLeaderId())
                    .setTimestamp(System.currentTimeMillis())
                    .build();
            responseObserver.onNext(ack);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error registering cluster: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void submitClusterSummary(ClusterSummaryRequest request, StreamObserver<Ack> responseObserver) {
        try {
            String vcJson = objectMapper.writeValueAsString(request.getVectorClockMap());

            ReadingRecord record = new ReadingRecord();
            record.setReadingId(request.getSummaryId().isEmpty() ? UUID.randomUUID().toString() : request.getSummaryId());
            record.setClusterId(request.getClusterId());
            record.setLeaderId(request.getLeaderId());
            record.setTimestamp(request.getWindowEndTime() > 0 ? request.getWindowEndTime() : System.currentTimeMillis());
            record.setSampleCount(request.getSampleCount());
            record.setAvgTemperature(request.getAvgTemperature());
            record.setMaxTemperature(request.getMaxTemperature());
            record.setMinTemperature(request.getMinTemperature());
            record.setAvgHumidity(request.getAvgHumidity());
            record.setAvgWaterPh(request.getAvgWaterPh());
            record.setAvgWaterTurbidity(request.getAvgWaterTurbidity());
            record.setMotionEventCount(request.getMotionEventCount());
            record.setVectorClockJson(vcJson);

            readingRepository.save(record);

            // Update cluster leader if changed
            clusterRepository.findById(request.getClusterId()).ifPresent(c -> {
                if (!request.getLeaderId().isEmpty() && !request.getLeaderId().equals(c.getLeaderId())) {
                    c.setLeaderId(request.getLeaderId());
                    clusterRepository.save(c);
                }
            });

            // Broadcast to dashboard
            Map<String, Object> summaryPayload = new HashMap<>();
            summaryPayload.put("summaryId", record.getReadingId());
            summaryPayload.put("clusterId", record.getClusterId());
            summaryPayload.put("leaderId", record.getLeaderId());
            summaryPayload.put("timestamp", record.getTimestamp());
            summaryPayload.put("sampleCount", record.getSampleCount());
            summaryPayload.put("avgTemperature", record.getAvgTemperature());
            summaryPayload.put("maxTemperature", record.getMaxTemperature());
            summaryPayload.put("avgHumidity", record.getAvgHumidity());
            summaryPayload.put("avgWaterPh", record.getAvgWaterPh());
            summaryPayload.put("avgWaterTurbidity", record.getAvgWaterTurbidity());
            summaryPayload.put("motionEventCount", record.getMotionEventCount());
            summaryPayload.put("vectorClock", request.getVectorClockMap());

            webSocketHandler.broadcast("SUMMARY_RECEIVED", summaryPayload);

            Ack ack = Ack.newBuilder()
                    .setSuccess(true)
                    .setMessage("Summary recorded for cluster " + request.getClusterId())
                    .setTimestamp(System.currentTimeMillis())
                    .build();
            responseObserver.onNext(ack);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error submitting cluster summary: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void reportClusterEvent(ClusterEvent request, StreamObserver<Ack> responseObserver) {
        try {
            String vcJson = objectMapper.writeValueAsString(request.getVectorClockMap());
            long now = request.getTimestamp() > 0 ? request.getTimestamp() : System.currentTimeMillis();

            if ("ELECTION".equalsIgnoreCase(request.getEventType()) || "ELECTION_WON".equalsIgnoreCase(request.getEventType())) {
                ElectionRecord election = new ElectionRecord();
                String eid = request.getEventId().isEmpty() ? UUID.randomUUID().toString() : request.getEventId();
                if (electionRepository.existsById(eid)) {
                    eid = eid + "-" + UUID.randomUUID().toString().substring(0, 8);
                }
                election.setElectionId(eid);
                election.setClusterId(request.getClusterId());
                election.setInitiator(request.getNodeId());
                election.setElectedLeader(request.getNodeId());
                election.setStartedAt(now);
                election.setCompletedAt(now);
                election.setReason(request.getDescription());
                election.setDurationMs(250L);
                electionRepository.save(election);

                // Update cluster entity
                clusterRepository.findById(request.getClusterId()).ifPresent(c -> {
                    c.setLeaderId(request.getNodeId());
                    c.setStatus("ACTIVE");
                    c.setLastElectionTime(now);
                    clusterRepository.save(c);
                });
            } else if ("ALERT_MUTEX".equalsIgnoreCase(request.getEventType()) || "ALERT".equalsIgnoreCase(request.getEventType())) {
                AlertRecord alert = new AlertRecord();
                String aid = request.getEventId().isEmpty() ? UUID.randomUUID().toString() : request.getEventId();
                if (alertRepository.existsById(aid)) {
                    aid = aid + "-" + UUID.randomUUID().toString().substring(0, 8);
                }
                alert.setAlertId(aid);
                alert.setClusterId(request.getClusterId());
                alert.setEventType(request.getEventType());
                alert.setSeverity(request.getSeverity().isEmpty() ? "WARNING" : request.getSeverity());
                alert.setSourceSensor(request.getNodeId());
                alert.setEventTime(now);
                alert.setStatus(request.getIsDeduplicated() ? "DEDUPLICATED_MUTEX" : "TRIGGERED");
                alert.setDescription(request.getDescription());
                alert.setIsDeduplicated(request.getIsDeduplicated());
                alert.setVectorClockJson(vcJson);
                alertRepository.save(alert);
            } else if ("HEARTBEAT_STATUS".equalsIgnoreCase(request.getEventType())) {
                sensorRepository.findById(request.getNodeId()).ifPresent(s -> {
                    s.setStatus(request.getDescription()); // HEALTHY, SUSPECTED, FAILED
                    s.setLastHeartbeat(now);
                    sensorRepository.save(s);
                });
            }

            // Stream to frontend
            webSocketHandler.broadcast("CLUSTER_EVENT", Map.of(
                    "eventId", request.getEventId(),
                    "clusterId", request.getClusterId(),
                    "nodeId", request.getNodeId(),
                    "eventType", request.getEventType(),
                    "severity", request.getSeverity(),
                    "description", request.getDescription(),
                    "timestamp", now,
                    "isDeduplicated", request.getIsDeduplicated(),
                    "vectorClock", request.getVectorClockMap()
            ));

            Ack ack = Ack.newBuilder()
                    .setSuccess(true)
                    .setMessage("Event recorded: " + request.getEventType())
                    .setTimestamp(now)
                    .build();
            responseObserver.onNext(ack);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error reporting cluster event: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getClusterState(ClusterStateRequest request, StreamObserver<ClusterStateResponse> responseObserver) {
        try {
            ClusterGroupEntity cluster = clusterRepository.findById(request.getClusterId()).orElse(null);
            ClusterStateResponse.Builder response = ClusterStateResponse.newBuilder()
                    .setClusterId(request.getClusterId())
                    .setLeaderId(cluster != null ? cluster.getLeaderId() : "UNKNOWN")
                    .setStatus(cluster != null ? cluster.getStatus() : "INACTIVE")
                    .setLastElectionTime(cluster != null && cluster.getLastElectionTime() != null ? cluster.getLastElectionTime() : 0);

            List<SensorNodeEntity> members = sensorRepository.findByClusterId(request.getClusterId());
            for (SensorNodeEntity m : members) {
                response.addMemberNodes(NodeState.newBuilder()
                        .setNodeId(m.getSensorId())
                        .setSensorType(m.getType() != null ? m.getType() : "SENSOR")
                        .setStatus(m.getStatus() != null ? m.getStatus() : "HEALTHY")
                        .setBatteryScore(m.getBatteryScore() != null ? m.getBatteryScore() : 100.0)
                        .setLastHeartbeat(m.getLastHeartbeat() != null ? m.getLastHeartbeat() : 0)
                        .build());
            }

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting cluster state: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }
}
