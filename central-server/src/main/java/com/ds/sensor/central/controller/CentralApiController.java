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

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CentralApiController {

    private final CentralService centralService;
    private final GlobalStateService globalStateService;
    private final AggregationStorageService aggregationStorageService;

    @Autowired
    public CentralApiController(CentralService centralService,
                                GlobalStateService globalStateService,
                                AggregationStorageService aggregationStorageService) {
        this.centralService = centralService;
        this.globalStateService = globalStateService;
        this.aggregationStorageService = aggregationStorageService;
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
}
