package com.ds.sensor.node.service;

import com.ds.sensor.node.config.NodeConfig;
import com.ds.sensor.shared.message.AggregatedData;
import com.ds.sensor.shared.message.AlertData;
import com.ds.sensor.shared.message.Message;
import com.ds.sensor.shared.message.MessageType;
import com.ds.sensor.shared.message.SensorReadingData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class CommunicationService {

    private static final Logger log = LoggerFactory.getLogger(CommunicationService.class);

    private final NodeConfig nodeConfig;
    private final RestTemplate restTemplate;
    private final Deque<Map<String, Object>> recentLogs = new ConcurrentLinkedDeque<>();
    private static final int MAX_LOGS = 50;

    @Autowired
    public CommunicationService(NodeConfig nodeConfig, RestTemplate restTemplate) {
        this.nodeConfig = nodeConfig;
        this.restTemplate = restTemplate;
    }

    public static String inferNodeIdFromUrl(String url) {
        if (url == null) return "UNKNOWN";
        if (url.contains("8101") || url.contains("sensor-1")) return "S1";
        if (url.contains("8102") || url.contains("sensor-2")) return "S2";
        if (url.contains("8103") || url.contains("sensor-3")) return "S3";
        if (url.contains("8104") || url.contains("sensor-4")) return "S4";
        if (url.contains("8000") || url.contains("central")) return "CENTRAL";
        return "PEER";
    }

    public Message sendToPeer(String peerUrl, Message message) {
        if (peerUrl == null || message == null) return null;
        if (message.getReceiverId() == null || message.getReceiverId().trim().isEmpty() || "null".equalsIgnoreCase(message.getReceiverId())) {
            message.setReceiverId(inferNodeIdFromUrl(peerUrl));
        }
        String targetEndpoint = peerUrl.replaceAll("/$", "") + "/api/message";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Message> request = new HttpEntity<>(message, headers);

            logLocal(message.getSenderId(), message.getReceiverId(), message.getMessageType().name(),
                    message.getVectorClock() != null ? message.getVectorClock().toString() : "[]",
                    "OUT -> " + peerUrl + ": " + (message.getPayload() != null ? message.getPayload().toString() : ""));

            // Also log to Central Server asynchronously
            logToCentralAsync(message);

            ResponseEntity<Message> response = restTemplate.postForEntity(targetEndpoint, request, Message.class);
            return response.getBody();
        } catch (Exception ex) {
            log.debug("Failed sending {} to peer {}: {}", message.getMessageType(), peerUrl, ex.getMessage());
            return null;
        }
    }

    public void logToCentralAsync(Message message) {
        new Thread(() -> {
            try {
                String centralEndpoint = nodeConfig.getCentralUrl().replaceAll("/$", "") + "/api/log-message";
                restTemplate.postForEntity(centralEndpoint, message, Map.class);
            } catch (Exception ignored) {
            }
        }).start();
    }

    public void sendHeartbeatToCentral() {
        sendHeartbeatToCentral(null, null, null, null, null);
    }

    public void sendHeartbeatToCentral(Double temp, Double hum, Double batt, Boolean motion, String vectorClock) {
        try {
            String url = nodeConfig.getCentralUrl().replaceAll("/$", "") + "/api/heartbeat";
            Map<String, Object> body = new HashMap<>();
            body.put("nodeId", nodeConfig.getNodeId());
            if (temp != null) body.put("temperature", temp);
            if (hum != null) body.put("humidity", hum);
            if (batt != null) body.put("battery", batt);
            if (motion != null) body.put("motion", motion);
            if (vectorClock != null) body.put("vectorClock", vectorClock);
            restTemplate.postForEntity(url, body, Map.class);
        } catch (Exception ex) {
            log.debug("Heartbeat to Central Server failed: {}", ex.getMessage());
        }
    }

    public void notifyCentralClusterHead(String clusterId, String headId, int port) {
        try {
            String url = nodeConfig.getCentralUrl().replaceAll("/$", "") + "/api/cluster-head";
            Map<String, Object> body = Map.of(
                    "clusterId", clusterId,
                    "headId", headId,
                    "port", port
            );
            restTemplate.postForEntity(url, body, Map.class);
            log.info("Notified Central Server: Node {} is new Cluster Head on port {}", headId, port);
        } catch (Exception ex) {
            log.warn("Failed to notify Central Server of cluster head change: {}", ex.getMessage());
        }
    }

    public void notifyCentralNodeStatus(String nodeId, String status) {
        try {
            String url = nodeConfig.getCentralUrl().replaceAll("/$", "") + "/api/node-status";
            Map<String, String> body = Map.of("nodeId", nodeId, "status", status);
            restTemplate.postForEntity(url, body, Map.class);
            log.info("Notified Central Server: Node {} status is {}", nodeId, status);
        } catch (Exception ex) {
            log.debug("Failed to notify Central Server of node status change: {}", ex.getMessage());
        }
    }

    public void sendElectionEventToCentral(String clusterId, String initiatorId, String leaderId, String eventType, String details) {
        try {
            String url = nodeConfig.getCentralUrl().replaceAll("/$", "") + "/api/election-event";
            Map<String, String> body = Map.of(
                    "clusterId", clusterId,
                    "initiatorId", initiatorId,
                    "leaderId", leaderId,
                    "eventType", eventType,
                    "details", details
            );
            restTemplate.postForEntity(url, body, Map.class);
        } catch (Exception ex) {
            log.debug("Failed to log election event to Central Server: {}", ex.getMessage());
        }
    }

    public void sendAggregatedDataToCentral(AggregatedData data) {
        try {
            String url = nodeConfig.getCentralUrl().replaceAll("/$", "") + "/api/aggregate";
            restTemplate.postForEntity(url, data, Map.class);
            log.info("Successfully sent AggregatedData to Central Server from cluster {}", data.getClusterId());

            Message logMsg = new Message();
            logMsg.setSenderId(nodeConfig.getNodeId());
            logMsg.setReceiverId("CENTRAL");
            logMsg.setClusterId(data.getClusterId());
            logMsg.setMessageType(MessageType.AGGREGATED_DATA);
            logMsg.setPayload("Aggregated batch: " + data.getSampleCount() + " samples, AvgTemp=" + String.format(Locale.US, "%.1f", data.getAvgTemperature()) + "°C");
            logToCentralAsync(logMsg);
        } catch (Exception ex) {
            log.warn("Failed sending AggregatedData to Central: {}", ex.getMessage());
        }
    }

    public void sendAlertToCentral(AlertData alertData) {
        try {
            String url = nodeConfig.getCentralUrl().replaceAll("/$", "") + "/api/alert";
            restTemplate.postForEntity(url, alertData, Map.class);
            log.info("Successfully reported Alert to Central Server: {}", alertData.getAlertType());

            Message logMsg = new Message();
            logMsg.setSenderId(nodeConfig.getNodeId());
            logMsg.setReceiverId("CENTRAL");
            logMsg.setClusterId(alertData.getClusterId());
            logMsg.setMessageType(MessageType.ALERT_REQUEST);
            logMsg.setVectorClock(alertData.getVectorClock());
            logMsg.setPayload("Alert: " + alertData.getAlertType() + " - " + alertData.getDescription());
            logToCentralAsync(logMsg);
        } catch (Exception ex) {
            log.warn("Failed sending Alert to Central: {}", ex.getMessage());
        }
    }

    public void sendReadingToCentral(SensorReadingData readingData) {
        try {
            String url = nodeConfig.getCentralUrl().replaceAll("/$", "") + "/api/reading";
            restTemplate.postForEntity(url, readingData, Map.class);
        } catch (Exception ex) {
            log.debug("Failed sending raw reading to Central: {}", ex.getMessage());
        }
    }

    public void sendEventToCentral(String eventType, String clusterId, String sourceNodeId, String description, String vectorClock) {
        try {
            String url = nodeConfig.getCentralUrl().replaceAll("/$", "") + "/api/event";
            Map<String, String> body = Map.of(
                    "eventType", eventType,
                    "clusterId", clusterId,
                    "sourceNodeId", sourceNodeId,
                    "description", description,
                    "vectorClock", vectorClock
            );
            restTemplate.postForEntity(url, body, Map.class);
        } catch (Exception ex) {
            log.debug("Failed logging event to Central: {}", ex.getMessage());
        }
    }

    public void logLocal(String sender, String receiver, String type, String vc, String summary) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("timestamp", System.currentTimeMillis());
        entry.put("sender", sender);
        entry.put("receiver", receiver);
        entry.put("type", type);
        entry.put("vectorClock", vc);
        entry.put("summary", summary);

        recentLogs.addFirst(entry);
        while (recentLogs.size() > MAX_LOGS) {
            recentLogs.removeLast();
        }
    }

    public List<Map<String, Object>> getRecentLogs() {
        return new ArrayList<>(recentLogs);
    }
}
