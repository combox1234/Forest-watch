package com.ds.sensor.node.service;

import com.ds.sensor.node.config.NodeConfig;
import com.ds.sensor.shared.message.Message;
import com.ds.sensor.shared.message.MessageType;
import com.ds.sensor.shared.message.SensorReadingData;
import com.ds.sensor.shared.message.VectorClockData;
import com.ds.sensor.shared.model.NodeRole;
import com.ds.sensor.shared.model.NodeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SensorService {

    private static final Logger log = LoggerFactory.getLogger(SensorService.class);

    private final NodeConfig nodeConfig;
    private final VectorClockService vectorClockService;
    private final CommunicationService communicationService;
    private final ElectionService electionService;
    private final HeartbeatService heartbeatService;
    private final EdgeAggregationService edgeAggregationService;
    private final MutualExclusionService mutualExclusionService;

    private NodeStatus status = NodeStatus.ACTIVE;
    private double temperature = 24.5;
    private double humidity = 55.0;
    private double battery = 98.0;
    private boolean motionDetected = false;
    private double connectivity = 100.0;
    private SensorReadingData lastReading;

    @Autowired
    public SensorService(NodeConfig nodeConfig,
                         VectorClockService vectorClockService,
                         CommunicationService communicationService,
                         ElectionService electionService,
                         HeartbeatService heartbeatService,
                         EdgeAggregationService edgeAggregationService,
                         MutualExclusionService mutualExclusionService) {
        this.nodeConfig = nodeConfig;
        this.vectorClockService = vectorClockService;
        this.communicationService = communicationService;
        this.electionService = electionService;
        this.heartbeatService = heartbeatService;
        this.edgeAggregationService = edgeAggregationService;
        this.mutualExclusionService = mutualExclusionService;

        // Initialize base values per node
        switch (nodeConfig.getNodeId()) {
            case "S1" -> { temperature = 23.2; humidity = 52.0; }
            case "S2" -> { temperature = 24.8; humidity = 56.0; }
            case "S3" -> { temperature = 28.1; humidity = 68.0; }
            case "S4" -> { temperature = 29.5; humidity = 71.0; }
        }
    }

    public synchronized SensorReadingData generateReading() {
        if (status == NodeStatus.FAILED) return null;

        // Simulate environmental jitter
        double tempDelta = (ThreadLocalRandom.current().nextDouble() - 0.48) * 0.4;
        temperature = Math.round((temperature + tempDelta) * 10.0) / 10.0;
        humidity = Math.round(Math.min(99.0, Math.max(20.0, humidity + (ThreadLocalRandom.current().nextDouble() - 0.5))) * 10.0) / 10.0;
        battery = Math.max(5.0, Math.round((battery - 0.02) * 100.0) / 100.0);

        VectorClockData vc = vectorClockService.tick();

        SensorReadingData reading = new SensorReadingData(
                nodeConfig.getNodeId(),
                nodeConfig.getClusterId(),
                temperature,
                humidity,
                motionDetected,
                battery,
                connectivity,
                vc
        );

        this.lastReading = reading;

        if (electionService.isClusterHead()) {
            // As Cluster Head: store in edge buffer
            edgeAggregationService.recordReading(reading);
        } else {
            // As SENSOR: forward SENSOR_DATA message to the Cluster Head peer
            List<String> peers = nodeConfig.getPeerUrls();
            if (!peers.isEmpty()) {
                String chUrl = peers.get(0);
                Message msg = new Message();
                msg.setSenderId(nodeConfig.getNodeId());
                msg.setReceiverId(CommunicationService.inferNodeIdFromUrl(chUrl));
                msg.setClusterId(nodeConfig.getClusterId());
                msg.setMessageType(MessageType.SENSOR_DATA);
                msg.setVectorClock(vc);
                msg.setPayload(reading);

                communicationService.sendToPeer(chUrl, msg);
            }
        }

        return reading;
    }

    public Map<String, Object> generateEvent(String eventType) {
        if (status == NodeStatus.FAILED) {
            return Map.of("error", "Node is currently FAILED");
        }

        this.motionDetected = true;
        VectorClockData vc = vectorClockService.tick();

        // 1. Send Event log to Central Server
        communicationService.sendEventToCentral(
                eventType,
                nodeConfig.getClusterId(),
                nodeConfig.getNodeId(),
                "Event " + eventType + " detected at node " + nodeConfig.getNodeId(),
                vc.toString()
        );

        // 2. Trigger Mutex Alert Deduplication flow
        List<String> peers = nodeConfig.getPeerUrls();
        String chUrl = peers.isEmpty() ? null : peers.get(0);
        Map<String, Object> mutexResult = mutualExclusionService.triggerAlert(eventType, electionService.isClusterHead(), chUrl);

        // Reset motion after 2 seconds
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                this.motionDetected = false;
            } catch (InterruptedException ignored) {}
        }).start();

        return mutexResult;
    }

    public void simulateFailure() {
        this.status = NodeStatus.FAILED;
        heartbeatService.setSimulatedFailed(true);
        log.warn("SIMULATE FAILURE: Node {} on port {} has CRASHED (simulated)!",
                nodeConfig.getNodeId(), nodeConfig.getPort());
        communicationService.notifyCentralNodeStatus(nodeConfig.getNodeId(), NodeStatus.FAILED.name());
    }

    public void recoverNode() {
        this.status = NodeStatus.ACTIVE;
        heartbeatService.setSimulatedFailed(false);
        log.info("NODE RECOVERED: Node {} on port {} is active again!",
                nodeConfig.getNodeId(), nodeConfig.getPort());

        // Inform Central Server of recovery
        communicationService.notifyCentralNodeStatus(nodeConfig.getNodeId(), NodeStatus.ACTIVE.name());
        communicationService.sendHeartbeatToCentral();

        // Run recovery logic (checks Bully priority to reclaim Cluster Head)
        electionService.onNodeRecovery();
    }

    @Scheduled(fixedRate = 4000)
    public void periodicSensorTask() {
        if (status == NodeStatus.FAILED) return;

        // Automatically generate reading periodically
        generateReading();

        // If this node is Cluster Head, perform sliding-window edge aggregation every 8 seconds
        if (electionService.isClusterHead() && ThreadLocalRandom.current().nextBoolean()) {
            edgeAggregationService.computeAndSendAggregation(lastReading);
        }
    }

    public Map<String, Object> getFullStatus() {
        Map<String, Object> map = new HashMap<>();
        map.put("nodeId", nodeConfig.getNodeId());
        map.put("clusterId", nodeConfig.getClusterId());
        map.put("port", nodeConfig.getPort());
        map.put("status", status.name());
        map.put("role", electionService.getCurrentRole().name());
        map.put("currentLeaderId", electionService.getCurrentLeaderId());
        map.put("isClusterHead", electionService.isClusterHead());
        map.put("temperature", temperature);
        map.put("humidity", humidity);
        map.put("battery", battery);
        map.put("motion", motionDetected);
        map.put("connectivity", connectivity);
        map.put("vectorClock", vectorClockService.getCurrentClock().getClock());
        map.put("vectorClockString", vectorClockService.getCurrentClockString());
        map.put("peerStatuses", heartbeatService.getPeerStatuses());
        map.put("peerUrls", nodeConfig.getPeerUrls());
        map.put("centralUrl", nodeConfig.getCentralUrl());
        return map;
    }

    public NodeStatus getStatus() { return status; }
    public SensorReadingData getLastReading() { return lastReading; }
}
