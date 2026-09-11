package com.ds.sensor.node.service;

import com.ds.sensor.node.config.NodeConfig;
import com.ds.sensor.shared.message.Message;
import com.ds.sensor.shared.message.MessageType;
import com.ds.sensor.shared.message.VectorClockData;
import com.ds.sensor.shared.model.NodeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class HeartbeatService {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatService.class);

    private final NodeConfig nodeConfig;
    private final CommunicationService communicationService;
    private final VectorClockService vectorClockService;
    private final ElectionService electionService;

    private final Map<String, Long> lastHeartbeats = new ConcurrentHashMap<>();
    private final Map<String, NodeStatus> peerStatuses = new ConcurrentHashMap<>();
    private final AtomicBoolean isSimulatedFailed = new AtomicBoolean(false);

    private static final long FAILURE_TIMEOUT_MS = 5000; // 5 seconds heartbeat timeout

    @Autowired
    public HeartbeatService(NodeConfig nodeConfig,
                            CommunicationService communicationService,
                            VectorClockService vectorClockService,
                            ElectionService electionService) {
        this.nodeConfig = nodeConfig;
        this.communicationService = communicationService;
        this.vectorClockService = vectorClockService;
        this.electionService = electionService;
    }

    @Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private SensorService sensorService;

    public void setSimulatedFailed(boolean failed) {
        this.isSimulatedFailed.set(failed);
    }

    public boolean isSimulatedFailed() {
        return this.isSimulatedFailed.get();
    }

    public void recordIncomingHeartbeat(String senderId, VectorClockData vc) {
        lastHeartbeats.put(senderId, System.currentTimeMillis());
        peerStatuses.put(senderId, NodeStatus.ACTIVE);
        if (vc != null) {
            vectorClockService.mergeAndTick(vc);
        }
    }

    @Scheduled(fixedRate = 2000)
    public void executeHeartbeatCycle() {
        if (isSimulatedFailed.get()) {
            return; // Crash simulation: silence heartbeats
        }

        // 1. Send heartbeat to Central Server with live telemetry
        com.ds.sensor.shared.message.SensorReadingData lr = sensorService != null ? sensorService.getLastReading() : null;
        if (lr != null) {
            communicationService.sendHeartbeatToCentral(
                    lr.getTemperature(),
                    lr.getHumidity(),
                    lr.getBatteryLevel(),
                    lr.isMotionDetected(),
                    lr.getVectorClock() != null ? lr.getVectorClock().toString() : "[]"
            );
        } else {
            communicationService.sendHeartbeatToCentral();
        }

        // 2. Send heartbeat to cluster peers
        List<String> peerUrls = nodeConfig.getPeerUrls();
        VectorClockData vc = vectorClockService.tick();

        for (String peerUrl : peerUrls) {
            Message hbMsg = new Message();
            hbMsg.setSenderId(nodeConfig.getNodeId());
            hbMsg.setReceiverId(CommunicationService.inferNodeIdFromUrl(peerUrl));
            hbMsg.setClusterId(nodeConfig.getClusterId());
            hbMsg.setMessageType(MessageType.HEARTBEAT);
            hbMsg.setVectorClock(vc);
            hbMsg.setPayload("Liveness ping from " + nodeConfig.getNodeId());

            Message ack = communicationService.sendToPeer(peerUrl, hbMsg);
            if (ack != null && (ack.getMessageType() == MessageType.HEARTBEAT_ACK || ack.getMessageType() == MessageType.ANSWER)) {
                String peerId = ack.getSenderId();
                if (peerId != null) {
                    lastHeartbeats.put(peerId, System.currentTimeMillis());
                    peerStatuses.put(peerId, NodeStatus.ACTIVE);
                }
            }
        }

        // 3. Liveness check on current cluster head and peers
        long now = System.currentTimeMillis();
        String currentLeader = electionService.getCurrentLeaderId();

        for (Map.Entry<String, Long> entry : lastHeartbeats.entrySet()) {
            String peerId = entry.getKey();
            long lastHb = entry.getValue();
            long diff = now - lastHb;

            if (diff > FAILURE_TIMEOUT_MS) {
                NodeStatus prev = peerStatuses.get(peerId);
                peerStatuses.put(peerId, NodeStatus.FAILED);

                if (prev != NodeStatus.FAILED) {
                    log.warn("Node {} detected FAILURE of peer {} (no heartbeat for {}ms)",
                            nodeConfig.getNodeId(), peerId, diff);
                }

                // If the failed peer is the current Cluster Head, trigger Bully Election!
                if (peerId.equalsIgnoreCase(currentLeader) && !electionService.isClusterHead()) {
                    log.error("CURRENT CLUSTER HEAD {} HAS FAILED! Node {} is launching Bully Election!",
                            peerId, nodeConfig.getNodeId());
                    electionService.startElection("Leader " + peerId + " missed heartbeats for >5000ms");
                }
            }
        }
    }

    public Map<String, NodeStatus> getPeerStatuses() {
        return new HashMap<>(peerStatuses);
    }

    public long getLastHeartbeatTime(String peerId) {
        return lastHeartbeats.getOrDefault(peerId, 0L);
    }
}
