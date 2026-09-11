package com.ds.sensor.node.service;

import com.ds.sensor.node.config.NodeConfig;
import com.ds.sensor.shared.message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MutualExclusionService {

    private static final Logger log = LoggerFactory.getLogger(MutualExclusionService.class);

    private final NodeConfig nodeConfig;
    private final CommunicationService communicationService;
    private final VectorClockService vectorClockService;

    // Cache of recent active alerts on Cluster Head: eventType -> AlertData
    private final Map<String, AlertData> activeAlerts = new ConcurrentHashMap<>();
    private static final long DEDUPLICATION_WINDOW_MS = 10000; // 10 seconds

    @Autowired
    public MutualExclusionService(NodeConfig nodeConfig,
                                  CommunicationService communicationService,
                                  VectorClockService vectorClockService) {
        this.nodeConfig = nodeConfig;
        this.communicationService = communicationService;
        this.vectorClockService = vectorClockService;
    }

    /**
     * Executed by Cluster Head to coordinate alert actions and deduplicate concurrent alerts.
     */
    public synchronized Message handleAlertRequest(Message incomingRequest) {
        String eventType = (String) incomingRequest.getPayload();
        if (eventType == null) eventType = "UNKNOWN_ALERT";

        long now = System.currentTimeMillis();
        AlertData existing = activeAlerts.get(eventType);

        // Vector clock tick & merge
        VectorClockData currentVc = vectorClockService.mergeAndTick(incomingRequest.getVectorClock());

        Message response = new Message();
        response.setSenderId(nodeConfig.getNodeId());
        response.setReceiverId(incomingRequest.getSenderId());
        response.setClusterId(nodeConfig.getClusterId());
        response.setVectorClock(currentVc);

        if (existing == null || (now - existing.getTimestamp()) > DEDUPLICATION_WINDOW_MS) {
            // Permission GRANTED: First detection in window
            log.info("MUTEX GRANTED: Node {} allowed to execute alert '{}'", incomingRequest.getSenderId(), eventType);
            AlertData newAlert = new AlertData();
            newAlert.setAlertId(UUID.randomUUID().toString());
            newAlert.setClusterId(nodeConfig.getClusterId());
            newAlert.setEventType(eventType);
            newAlert.setSourceNodeId(incomingRequest.getSenderId());
            newAlert.setDescription("Critical Event '" + eventType + "' confirmed by Cluster Head " + nodeConfig.getNodeId());
            newAlert.setSeverity("CRITICAL");
            newAlert.setStatus("TRIGGERED");
            newAlert.setVectorClock(currentVc);

            activeAlerts.put(eventType, newAlert);

            // Forward to Central Server
            communicationService.sendAlertToCentral(newAlert);

            response.setMessageType(MessageType.ALERT_GRANTED);
            response.setPayload("Alert granted and dispatched to Central Server");
        } else {
            // Permission REJECTED: Duplicate concurrent event detected
            log.info("MUTEX REJECTED (Deduplicated): Node {} duplicate alert '{}' suppressed. Primary source: {}",
                    incomingRequest.getSenderId(), eventType, existing.getSourceNodes());

            // Merge this node into existing alert sources at Central Server
            existing.setSourceNodeId(incomingRequest.getSenderId());
            communicationService.sendAlertToCentral(existing);

            response.setMessageType(MessageType.ALERT_REJECTED);
            response.setPayload("Alert deduplicated. Concurrent event already handled for source " + existing.getSourceNodes());
        }

        return response;
    }

    /**
     * Executed by any node (SENSOR or CLUSTER_HEAD) to trigger an alert.
     */
    public Map<String, Object> triggerAlert(String eventType, boolean isClusterHead, String clusterHeadUrl) {
        VectorClockData vc = vectorClockService.tick();

        Map<String, Object> result = new HashMap<>();
        result.put("eventType", eventType);
        result.put("sender", nodeConfig.getNodeId());
        result.put("vectorClock", vc.toString());

        if (isClusterHead) {
            // Local processing as Cluster Head
            Message syntheticMsg = new Message();
            syntheticMsg.setSenderId(nodeConfig.getNodeId());
            syntheticMsg.setReceiverId(nodeConfig.getNodeId());
            syntheticMsg.setClusterId(nodeConfig.getClusterId());
            syntheticMsg.setMessageType(MessageType.ALERT_REQUEST);
            syntheticMsg.setVectorClock(vc);
            syntheticMsg.setPayload(eventType);

            Message resp = handleAlertRequest(syntheticMsg);
            result.put("status", resp.getMessageType().name());
            result.put("message", resp.getPayload());
        } else {
            // Forward ALERT_REQUEST to Cluster Head peer
            Message request = new Message();
            request.setSenderId(nodeConfig.getNodeId());
            request.setReceiverId(CommunicationService.inferNodeIdFromUrl(clusterHeadUrl));
            request.setClusterId(nodeConfig.getClusterId());
            request.setMessageType(MessageType.ALERT_REQUEST);
            request.setVectorClock(vc);
            request.setPayload(eventType);

            Message response = communicationService.sendToPeer(clusterHeadUrl, request);
            if (response != null) {
                vectorClockService.mergeAndTick(response.getVectorClock());
                result.put("status", response.getMessageType().name());
                result.put("message", response.getPayload());
            } else {
                result.put("status", "FAILED_NO_RESPONSE");
                result.put("message", "Cluster Head did not respond to alert request");
            }
        }

        return result;
    }
}
