package com.ds.sensor.node.controller;

import com.ds.sensor.node.config.NodeConfig;
import com.ds.sensor.node.service.*;
import com.ds.sensor.shared.message.*;
import com.ds.sensor.shared.model.NodeStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SensorApiController {

    private static final Logger log = LoggerFactory.getLogger(SensorApiController.class);

    private final NodeConfig nodeConfig;
    private final SensorService sensorService;
    private final VectorClockService vectorClockService;
    private final CommunicationService communicationService;
    private final ElectionService electionService;
    private final HeartbeatService heartbeatService;
    private final EdgeAggregationService edgeAggregationService;
    private final MutualExclusionService mutualExclusionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public SensorApiController(NodeConfig nodeConfig,
                               SensorService sensorService,
                               VectorClockService vectorClockService,
                               CommunicationService communicationService,
                               ElectionService electionService,
                               HeartbeatService heartbeatService,
                               EdgeAggregationService edgeAggregationService,
                               MutualExclusionService mutualExclusionService) {
        this.nodeConfig = nodeConfig;
        this.sensorService = sensorService;
        this.vectorClockService = vectorClockService;
        this.communicationService = communicationService;
        this.electionService = electionService;
        this.heartbeatService = heartbeatService;
        this.edgeAggregationService = edgeAggregationService;
        this.mutualExclusionService = mutualExclusionService;
    }

    /**
     * Universal incoming message handler for inter-node communication.
     */
    @PostMapping("/message")
    public ResponseEntity<Message> receiveMessage(@RequestBody Message message) {
        // If this node is simulated as crashed, refuse incoming requests
        if (sensorService.getStatus() == NodeStatus.FAILED) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        if (message == null || message.getMessageType() == null) {
            return ResponseEntity.badRequest().build();
        }

        // Log locally
        String vcStr = message.getVectorClock() != null ? message.getVectorClock().toString() : "[]";
        communicationService.logLocal(message.getSenderId(), nodeConfig.getNodeId(),
                message.getMessageType().name(), vcStr,
                "IN <- " + message.getSenderId() + ": " + (message.getPayload() != null ? message.getPayload().toString() : ""));

        MessageType type = message.getMessageType();

        switch (type) {
            case HEARTBEAT -> {
                heartbeatService.recordIncomingHeartbeat(message.getSenderId(), message.getVectorClock());
                Message ack = new Message();
                ack.setSenderId(nodeConfig.getNodeId());
                ack.setReceiverId(message.getSenderId());
                ack.setClusterId(nodeConfig.getClusterId());
                ack.setMessageType(MessageType.HEARTBEAT_ACK);
                ack.setVectorClock(vectorClockService.getCurrentClock());
                ack.setPayload("PONG");
                return ResponseEntity.ok(ack);
            }

            case ELECTION, COORDINATOR, ANSWER -> {
                Message electionResp = electionService.handleIncomingMessage(message);
                return ResponseEntity.ok(electionResp);
            }

            case ALERT_REQUEST -> {
                Message alertResp = mutualExclusionService.handleAlertRequest(message);
                return ResponseEntity.ok(alertResp);
            }

            case SENSOR_DATA -> {
                vectorClockService.merge(message.getVectorClock());
                try {
                    SensorReadingData data = objectMapper.convertValue(message.getPayload(), SensorReadingData.class);
                    edgeAggregationService.recordReading(data);
                } catch (Exception e) {
                    log.warn("Failed to parse incoming SensorReadingData: {}", e.getMessage());
                }

                Message ack = new Message();
                ack.setSenderId(nodeConfig.getNodeId());
                ack.setReceiverId(message.getSenderId());
                ack.setClusterId(nodeConfig.getClusterId());
                ack.setMessageType(MessageType.HEARTBEAT_ACK);
                ack.setVectorClock(vectorClockService.getCurrentClock());
                ack.setPayload("Reading received");
                return ResponseEntity.ok(ack);
            }

            default -> {
                Message genericAck = new Message();
                genericAck.setSenderId(nodeConfig.getNodeId());
                genericAck.setReceiverId(message.getSenderId());
                genericAck.setClusterId(nodeConfig.getClusterId());
                genericAck.setMessageType(MessageType.HEARTBEAT_ACK);
                genericAck.setVectorClock(vectorClockService.getCurrentClock());
                return ResponseEntity.ok(genericAck);
            }
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(sensorService.getFullStatus());
    }

    @PostMapping("/generate-reading")
    public ResponseEntity<SensorReadingData> generateReading() {
        SensorReadingData reading = sensorService.generateReading();
        return ResponseEntity.ok(reading);
    }

    @PostMapping("/generate-event")
    public ResponseEntity<Map<String, Object>> generateEvent(@RequestBody(required = false) Map<String, String> body) {
        String eventType = (body != null && body.containsKey("eventType")) ? body.get("eventType") : "ANIMAL_DETECTED";
        Map<String, Object> result = sensorService.generateEvent(eventType);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/start-election")
    public ResponseEntity<Map<String, String>> startElection() {
        electionService.startElection("Triggered manually via Sensor UI");
        return ResponseEntity.ok(Map.of("status", "ELECTION_INITIATED"));
    }

    @PostMapping("/simulate-failure")
    public ResponseEntity<Map<String, String>> simulateFailure() {
        sensorService.simulateFailure();
        return ResponseEntity.ok(Map.of("status", "FAILED", "nodeId", nodeConfig.getNodeId()));
    }

    @PostMapping("/recover")
    public ResponseEntity<Map<String, String>> recover() {
        sensorService.recoverNode();
        return ResponseEntity.ok(Map.of("status", "ACTIVE", "nodeId", nodeConfig.getNodeId()));
    }

    @PostMapping("/aggregate-and-send")
    public ResponseEntity<AggregatedData> aggregateAndSend() {
        AggregatedData agg = edgeAggregationService.computeAndSendAggregation(sensorService.getLastReading());
        return ResponseEntity.ok(agg);
    }

    @GetMapping("/logs")
    public ResponseEntity<List<Map<String, Object>>> getLogs() {
        return ResponseEntity.ok(communicationService.getRecentLogs());
    }

    @PostMapping({"/vector-clock/reset", "/sensor/vector-clock/reset"})
    public ResponseEntity<Map<String, String>> resetVectorClock() {
        sensorService.resetClock();
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "nodeId", nodeConfig.getNodeId(),
                "vectorClock", vectorClockService.getCurrentClockString(),
                "vectorClockFormatted", vectorClockService.getFormattedClockString()
        ));
    }

    @GetMapping("/auth/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String username = authentication.getName();
        String displayName = username;
        String role = "VIEWER";
        if ("priya.admin".equals(username)) {
            displayName = "Priya Sharma";
            role = "ADMINISTRATOR";
        } else if ("arjun.operator".equals(username)) {
            displayName = "Arjun Patil";
            role = "FOREST_OPERATOR";
        } else if ("ananya.viewer".equals(username)) {
            displayName = "Ananya Deshmukh";
            role = "VIEWER";
        } else if ("rahul.operator".equals(username)) {
            displayName = "Rahul Verma";
            role = "FOREST_OPERATOR";
        } else if ("kavya.viewer".equals(username)) {
            displayName = "Kavya Iyer";
            role = "VIEWER";
        } else if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATOR"))) {
            role = "ADMINISTRATOR";
        } else if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FOREST_OPERATOR"))) {
            role = "FOREST_OPERATOR";
        }

        List<String> authorities = authentication.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .toList();

        Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("username", username);
        resp.put("displayName", displayName);
        resp.put("role", role);
        resp.put("authorities", authorities);
        return ResponseEntity.ok(resp);
    }
}

