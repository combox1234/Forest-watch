package com.ds.sensor.central.controller;

import com.ds.sensor.central.model.*;
import com.ds.sensor.central.service.AggregationStorageService;
import com.ds.sensor.central.service.CentralService;
import com.ds.sensor.central.service.GlobalStateService;
import com.ds.sensor.shared.message.*;
import com.ds.sensor.shared.model.NodeRole;
import com.ds.sensor.shared.model.NodeStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CentralApiController {

    private final CentralService centralService;
    private final GlobalStateService globalStateService;
    private final AggregationStorageService aggregationStorageService;
    private final com.ds.sensor.central.service.AwsS3ArchiveService awsS3ArchiveService;
    private final com.ds.sensor.central.service.AwsLambdaDispatcherService awsLambdaDispatcherService;

    @Autowired
    public CentralApiController(CentralService centralService,
                                GlobalStateService globalStateService,
                                AggregationStorageService aggregationStorageService,
                                com.ds.sensor.central.service.AwsS3ArchiveService awsS3ArchiveService,
                                com.ds.sensor.central.service.AwsLambdaDispatcherService awsLambdaDispatcherService) {
        this.centralService = centralService;
        this.globalStateService = globalStateService;
        this.aggregationStorageService = aggregationStorageService;
        this.awsS3ArchiveService = awsS3ArchiveService;
        this.awsLambdaDispatcherService = awsLambdaDispatcherService;
    }

    @GetMapping("/system-status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        return ResponseEntity.ok(globalStateService.getSystemOverviewMetrics());
    }

    @GetMapping("/nodes")
    public ResponseEntity<List<NodeEntity>> getNodes() {
        return ResponseEntity.ok(centralService.getAllNodes());
    }

    @GetMapping("/clusters")
    public ResponseEntity<List<ClusterStateEntity>> getClusters() {
        return ResponseEntity.ok(centralService.getAllClusters());
    }

    @PostMapping("/nodes/register")
    public ResponseEntity<Map<String, Object>> registerNode(@RequestBody Map<String, Object> req) {
        String nodeId = (String) req.get("nodeId");
        int port = req.containsKey("port") ? ((Number) req.get("port")).intValue() : 0;
        String clusterId = (String) req.get("clusterId");
        String roleStr = (String) req.get("role");
        NodeRole role = roleStr != null ? NodeRole.valueOf(roleStr) : NodeRole.SENSOR;

        NodeEntity node = centralService.registerOrUpdateNode(nodeId, port, clusterId, NodeStatus.ACTIVE, role);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "node", node));
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Map<String, String>> receiveHeartbeat(@RequestBody Map<String, Object> req) {
        String nodeId = (String) req.get("nodeId");
        if (nodeId != null) {
            Double temp = req.containsKey("temperature") && req.get("temperature") != null ? ((Number) req.get("temperature")).doubleValue() : null;
            Double hum = req.containsKey("humidity") && req.get("humidity") != null ? ((Number) req.get("humidity")).doubleValue() : null;
            Double batt = req.containsKey("battery") && req.get("battery") != null ? ((Number) req.get("battery")).doubleValue() : null;
            Boolean motion = req.containsKey("motion") && req.get("motion") != null ? (Boolean) req.get("motion") : null;
            String vc = req.containsKey("vectorClock") && req.get("vectorClock") != null ? req.get("vectorClock").toString() : null;
            centralService.recordHeartbeat(nodeId, temp, hum, batt, motion, vc);
        }
        return ResponseEntity.ok(Map.of("status", "ACK", "timestamp", String.valueOf(System.currentTimeMillis())));
    }

    @PostMapping("/node-status")
    public ResponseEntity<Map<String, String>> updateNodeStatus(@RequestBody Map<String, String> req) {
        String nodeId = req.get("nodeId");
        String statusStr = req.get("status");
        if (nodeId != null && statusStr != null) {
            try {
                NodeStatus st = NodeStatus.valueOf(statusStr.toUpperCase());
                centralService.updateNodeStatus(nodeId, st);
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(Map.of("status", "UPDATED"));
    }

    @PostMapping("/cluster-head")
    public ResponseEntity<Map<String, String>> updateClusterHead(@RequestBody Map<String, Object> req) {
        String clusterId = (String) req.get("clusterId");
        String headId = (String) req.get("headId");
        int port = req.containsKey("port") ? ((Number) req.get("port")).intValue() : 0;

        centralService.updateClusterHead(clusterId, headId, port);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Cluster head updated to " + headId));
    }

    @PostMapping("/election-event")
    public ResponseEntity<Map<String, String>> recordElectionEvent(@RequestBody Map<String, String> req) {
        String clusterId = req.get("clusterId");
        String initiatorId = req.get("initiatorId");
        String leaderId = req.get("leaderId");
        String eventType = req.get("eventType");
        String details = req.get("details");

        centralService.recordElectionEvent(clusterId, initiatorId, leaderId, eventType, details);
        return ResponseEntity.ok(Map.of("status", "ACK"));
    }

    @PostMapping("/aggregate")
    public ResponseEntity<Map<String, Object>> receiveAggregatedData(@RequestBody AggregatedData data) {
        AggregatedReadingEntity saved = aggregationStorageService.saveAggregatedData(data);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "id", saved != null ? saved.getId() : -1));
    }

    @PostMapping("/reading")
    public ResponseEntity<Map<String, Object>> receiveRawReading(@RequestBody SensorReadingData data) {
        SensorReadingEntity saved = centralService.recordRawReading(data);
        if (data.getVectorClock() != null) {
            globalStateService.updateGlobalClock(data.getVectorClock());
        }
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "id", saved != null ? saved.getId() : -1));
    }

    @PostMapping("/event")
    public ResponseEntity<Map<String, Object>> receiveEvent(@RequestBody Map<String, String> req) {
        String eventType = req.get("eventType");
        String clusterId = req.get("clusterId");
        String sourceNodeId = req.get("sourceNodeId");
        String description = req.get("description");
        String vectorClock = req.get("vectorClock");

        EventEntity saved = centralService.recordEvent(eventType, clusterId, sourceNodeId, description, vectorClock);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "id", saved != null ? saved.getId() : -1));
    }

    @PostMapping("/alert")
    public ResponseEntity<Map<String, Object>> receiveAlert(@RequestBody AlertData alertData) {
        AlertEntity saved = centralService.recordAlert(alertData);
        if (alertData.getVectorClock() != null) {
            globalStateService.updateGlobalClock(alertData.getVectorClock());
        }
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "alertId", saved != null ? saved.getId() : -1, "sourceCount", saved != null ? saved.getSourceNodes().size() : 1));
    }

    @PostMapping("/log-message")
    public ResponseEntity<Map<String, String>> logMessage(@RequestBody Message message) {
        centralService.recordCommunicationLog(message);
        return ResponseEntity.ok(Map.of("status", "LOGGED"));
    }

    @GetMapping("/logs")
    public ResponseEntity<List<CommunicationLogEntity>> getRecentLogs() {
        return ResponseEntity.ok(centralService.getRecentLogs());
    }

    @GetMapping("/events")
    public ResponseEntity<List<EventEntity>> getRecentEvents() {
        return ResponseEntity.ok(centralService.getRecentEvents());
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<AlertEntity>> getRecentAlerts() {
        return ResponseEntity.ok(centralService.getRecentAlerts());
    }

    @GetMapping("/elections")
    public ResponseEntity<List<ElectionLogEntity>> getRecentElections() {
        return ResponseEntity.ok(centralService.getRecentElectionLogs());
    }

    @GetMapping("/aggregated-readings")
    public ResponseEntity<List<AggregatedReadingEntity>> getAggregatedReadings() {
        return ResponseEntity.ok(aggregationStorageService.getRecentAggregatedReadings());
    }

    // --- Unit IV: AWS Cloud Services & Emerging Paradigms ---

    @GetMapping("/aws/status")
    public ResponseEntity<Map<String, Object>> getAwsStatus() {
        return ResponseEntity.ok(Map.of(
                "s3Bucket", awsS3ArchiveService.getBucketName(),
                "lambdaFunction", awsLambdaDispatcherService.getLambdaFunctionName(),
                "awsRegion", awsS3ArchiveService.getAwsRegion(),
                "cloudProvider", "AWS",
                "status", "CONNECTED"
        ));
    }

    @GetMapping("/aws/s3-archives")
    public ResponseEntity<List<com.ds.sensor.central.model.S3ArchiveEntity>> getS3Archives() {
        return ResponseEntity.ok(awsS3ArchiveService.getRecentArchives());
    }

    @PostMapping("/aws/s3-archive")
    public ResponseEntity<Map<String, Object>> manualS3Archive(@RequestBody(required = false) Map<String, Object> req) {
        String clusterId = req != null && req.containsKey("clusterId") ? (String) req.get("clusterId") : "C1";
        String chId = req != null && req.containsKey("clusterHeadId") ? (String) req.get("clusterHeadId") : "S2";
        AggregatedData sample = new AggregatedData();
        sample.setClusterId(clusterId);
        sample.setClusterHeadId(chId);
        sample.setSampleCount(10);
        sample.setAvgTemperature(24.5 + Math.random() * 2);
        sample.setMinTemperature(21.0);
        sample.setMaxTemperature(27.5);
        sample.setAvgHumidity(55.0 + Math.random() * 5);
        sample.setActiveSensors(2);
        sample.setVectorClock(new VectorClockData(1, 2, 0, 0));
        com.ds.sensor.central.model.S3ArchiveEntity entity = awsS3ArchiveService.archiveAggregatedData(sample);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "archiveId", entity != null ? entity.getId() : -1, "objectKey", entity != null ? entity.getObjectKey() : ""));
    }

    @GetMapping("/aws/lambda-invocations")
    public ResponseEntity<List<com.ds.sensor.central.model.LambdaInvocationEntity>> getLambdaInvocations() {
        return ResponseEntity.ok(awsLambdaDispatcherService.getRecentInvocations());
    }

    @PostMapping("/aws/trigger-lambda")
    public ResponseEntity<Map<String, Object>> triggerLambda(@RequestBody(required = false) Map<String, String> req) {
        String alertType = req != null && req.containsKey("alertType") ? req.get("alertType") : "WILDFIRE_EMERGENCY";
        String clusterId = req != null && req.containsKey("clusterId") ? req.get("clusterId") : "C1";
        String sourceNode = req != null && req.containsKey("sourceNode") ? req.get("sourceNode") : "S1";
        String vc = req != null && req.containsKey("vectorClock") ? req.get("vectorClock") : "[1,0,0,0]";

        com.ds.sensor.central.model.LambdaInvocationEntity entity = awsLambdaDispatcherService.triggerManualTest(alertType, clusterId, sourceNode, vc);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "requestId", entity.getRequestId(),
                "durationMs", entity.getDurationMs(),
                "functionName", entity.getFunctionName()
        ));
    }

    // --- Unit IV: Kubernetes & Load Balancer Infrastructure ---

    private final AtomicLong requestCounter = new AtomicLong(1);
    private final Map<String, Long> podRequestCounts = new ConcurrentHashMap<>();

    @GetMapping("/load-balancer/dispatch")
    public ResponseEntity<Map<String, Object>> dispatchViaLoadBalancer() {
        long seq = requestCounter.getAndIncrement();
        String hostname = System.getenv("HOSTNAME");
        if (hostname == null || hostname.trim().isEmpty() || "null".equalsIgnoreCase(hostname)) {
            // Emulate alternating round-robin pods when running in standalone mode
            hostname = (seq % 2 == 1) ? "central-server-pod-alpha" : "central-server-pod-beta";
        }
        podRequestCounts.merge(hostname, 1L, Long::sum);

        return ResponseEntity.ok(Map.of(
                "requestSequence", seq,
                "handlingPod", hostname,
                "routingAlgorithm", "ROUND_ROBIN",
                "loadBalancerType", "KUBERNETES_SERVICE_LOADBALANCER",
                "podIp", (hostname.contains("alpha") ? "10.244.1.18" : "10.244.2.24"),
                "status", "HEALTHY",
                "timestamp", System.currentTimeMillis()
        ));
    }

    @GetMapping("/load-balancer/stats")
    public ResponseEntity<Map<String, Object>> getLoadBalancerStats() {
        return ResponseEntity.ok(Map.of(
                "totalRequestsRouted", requestCounter.get() - 1,
                "activeReplicas", 2,
                "algorithm", "ROUND_ROBIN",
                "healthCheckStatus", "HEALTHY_ALL_UP",
                "distribution", podRequestCounts
        ));
    }

    @PostMapping("/vector-clock/reset")
    public ResponseEntity<Map<String, Object>> resetVectorClock() {
        globalStateService.resetGlobalClock();
        centralService.broadcastVectorClockReset();
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Global and local vector clocks reset to [0, 0, 0, 0]",
                "globalVectorClock", globalStateService.getGlobalVectorClock().getClock(),
                "globalVectorClockFormatted", globalStateService.getGlobalVectorClock().toFormattedString()
        ));
    }
}
