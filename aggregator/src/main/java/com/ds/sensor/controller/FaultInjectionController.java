package com.ds.sensor.controller;

import com.ds.sensor.model.SensorNodeEntity;
import com.ds.sensor.repository.SensorNodeRepository;
import com.ds.sensor.websocket.DashboardWebSocketHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/simulation")
@CrossOrigin(origins = "*")
public class FaultInjectionController {

    private final SensorNodeRepository sensorRepository;
    private final DashboardWebSocketHandler webSocketHandler;

    public FaultInjectionController(
            SensorNodeRepository sensorRepository,
            DashboardWebSocketHandler webSocketHandler) {
        this.sensorRepository = sensorRepository;
        this.webSocketHandler = webSocketHandler;
    }

    @PostMapping("/fault/{nodeId}")
    public ResponseEntity<Map<String, Object>> injectFault(@PathVariable String nodeId) {
        Optional<SensorNodeEntity> opt = sensorRepository.findById(nodeId);
        if (opt.isPresent()) {
            SensorNodeEntity sensor = opt.get();
            sensor.setStatus("FAILED");
            sensorRepository.save(sensor);
        }

        // Notify all clients and simulated cluster head processes
        webSocketHandler.broadcast("SIMULATION_FAULT", Map.of(
                "action", "FAIL_NODE",
                "nodeId", nodeId,
                "timestamp", System.currentTimeMillis()
        ));

        return ResponseEntity.ok(Map.of(
                "success", true,
                "nodeId", nodeId,
                "status", "FAILED",
                "message", "Fault injected: node " + nodeId + " marked as FAILED"
        ));
    }

    @PostMapping("/recover/{nodeId}")
    public ResponseEntity<Map<String, Object>> recoverNode(@PathVariable String nodeId) {
        Optional<SensorNodeEntity> opt = sensorRepository.findById(nodeId);
        if (opt.isPresent()) {
            SensorNodeEntity sensor = opt.get();
            sensor.setStatus("HEALTHY");
            sensor.setLastHeartbeat(System.currentTimeMillis());
            sensorRepository.save(sensor);
        }

        webSocketHandler.broadcast("SIMULATION_RECOVER", Map.of(
                "action", "RECOVER_NODE",
                "nodeId", nodeId,
                "timestamp", System.currentTimeMillis()
        ));

        return ResponseEntity.ok(Map.of(
                "success", true,
                "nodeId", nodeId,
                "status", "HEALTHY",
                "message", "Node " + nodeId + " recovered"
        ));
    }

    @PostMapping("/trigger-alert")
    public ResponseEntity<Map<String, Object>> triggerAlert(
            @RequestParam(required = false) String clusterId,
            @RequestParam(required = false) String eventType,
            @RequestBody(required = false) Map<String, Object> body) {
        String effectiveClusterId = clusterId != null ? clusterId :
                (body != null && body.containsKey("clusterId") ? (String) body.get("clusterId") : "cluster-1");
        String effectiveEventType = eventType != null ? eventType :
                (body != null && body.containsKey("eventType") ? (String) body.get("eventType") : "WILDFIRE_ALERT");

        webSocketHandler.broadcast("SIMULATION_TRIGGER_ALERT", Map.of(
                "action", "CONCURRENT_ALERT",
                "clusterId", effectiveClusterId,
                "eventType", effectiveEventType,
                "timestamp", System.currentTimeMillis()
        ));

        return ResponseEntity.ok(Map.of(
                "success", true,
                "clusterId", effectiveClusterId,
                "eventType", effectiveEventType,
                "message", "Concurrent alert triggered on " + effectiveClusterId + " to demonstrate mutual exclusion"
        ));
    }

    @PostMapping("/delay")
    public ResponseEntity<Map<String, Object>> injectDelay(
            @RequestParam(required = false) String nodeId,
            @RequestParam(required = false) Integer delayMs,
            @RequestBody(required = false) Map<String, Object> body) {
        String effectiveNodeId = nodeId != null ? nodeId :
                (body != null && body.containsKey("nodeId") ? (String) body.get("nodeId") : "S1");
        int effectiveDelay = delayMs != null ? delayMs :
                (body != null && body.containsKey("delayMs") ? (Integer) body.get("delayMs") : 3000);

        webSocketHandler.broadcast("SIMULATION_DELAY", Map.of(
                "action", "INJECT_DELAY",
                "nodeId", effectiveNodeId,
                "delayMs", effectiveDelay,
                "timestamp", System.currentTimeMillis()
        ));

        return ResponseEntity.ok(Map.of(
                "success", true,
                "nodeId", effectiveNodeId,
                "delayMs", effectiveDelay,
                "message", "Network delay injected for " + effectiveNodeId + " to demonstrate vector clock causal reordering"
        ));
    }

    @PostMapping("/recover-all")
    public ResponseEntity<Map<String, Object>> recoverAll() {
        List<SensorNodeEntity> sensors = sensorRepository.findAll();
        for (SensorNodeEntity s : sensors) {
            s.setStatus("HEALTHY");
            s.setLastHeartbeat(System.currentTimeMillis());
            sensorRepository.save(s);
        }

        webSocketHandler.broadcast("SIMULATION_RECOVER_ALL", Map.of(
                "action", "RECOVER_ALL",
                "timestamp", System.currentTimeMillis()
        ));

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "All nodes marked healthy and recovered"
        ));
    }
}
