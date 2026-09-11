package com.ds.sensor.node.service;

import com.ds.sensor.node.config.NodeConfig;
import com.ds.sensor.shared.message.Message;
import com.ds.sensor.shared.message.MessageType;
import com.ds.sensor.shared.message.VectorClockData;
import com.ds.sensor.shared.model.NodeRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ElectionService {

    private static final Logger log = LoggerFactory.getLogger(ElectionService.class);

    private final NodeConfig nodeConfig;
    private final CommunicationService communicationService;
    private final VectorClockService vectorClockService;

    private final AtomicBoolean electionInProgress = new AtomicBoolean(false);
    private String currentLeaderId;
    private NodeRole currentRole;

    @Autowired
    public ElectionService(NodeConfig nodeConfig,
                           CommunicationService communicationService,
                           VectorClockService vectorClockService) {
        this.nodeConfig = nodeConfig;
        this.communicationService = communicationService;
        this.vectorClockService = vectorClockService;

        // Set initial leader & role based on config
        if (nodeConfig.isInitialLeader()) {
            this.currentRole = NodeRole.CLUSTER_HEAD;
            this.currentLeaderId = nodeConfig.getNodeId();
        } else {
            this.currentRole = NodeRole.SENSOR;
            // Default initial leaders for C1 and C2
            this.currentLeaderId = "C1".equalsIgnoreCase(nodeConfig.getClusterId()) ? "S2" : "S4";
        }
    }

    public int getNodePriority(String nodeId) {
        if (nodeId == null) return 0;
        return switch (nodeId.trim().toUpperCase()) {
            case "S4" -> 4;
            case "S3" -> 3;
            case "S2" -> 2;
            case "S1" -> 1;
            default -> {
                try {
                    yield Integer.parseInt(nodeId.replaceAll("\\D", ""));
                } catch (Exception e) {
                    yield 0;
                }
            }
        };
    }

    public synchronized void startElection(String reason) {
        int myPriority = getNodePriority(nodeConfig.getNodeId());
        log.info("Node {} (Priority {}) initiating Bully Election. Reason: {}",
                nodeConfig.getNodeId(), myPriority, reason);

        electionInProgress.set(true);
        VectorClockData vc = vectorClockService.tick();

        communicationService.sendElectionEventToCentral(
                nodeConfig.getClusterId(),
                nodeConfig.getNodeId(),
                currentLeaderId,
                "ELECTION_STARTED",
                "Node " + nodeConfig.getNodeId() + " started election: " + reason
        );

        List<String> peerUrls = nodeConfig.getPeerUrls();
        boolean receivedAnswerFromHigherNode = false;

        for (String peerUrl : peerUrls) {
            Message electionMsg = new Message();
            electionMsg.setSenderId(nodeConfig.getNodeId());
            electionMsg.setReceiverId(CommunicationService.inferNodeIdFromUrl(peerUrl));
            electionMsg.setClusterId(nodeConfig.getClusterId());
            electionMsg.setMessageType(MessageType.ELECTION);
            electionMsg.setVectorClock(vc);
            electionMsg.setPayload("Bully Election Started by " + nodeConfig.getNodeId());

            Message response = communicationService.sendToPeer(peerUrl, electionMsg);
            if (response != null && response.getMessageType() == MessageType.ANSWER) {
                log.info("Node {} received ANSWER from higher peer {}", nodeConfig.getNodeId(), response.getSenderId());
                receivedAnswerFromHigherNode = true;
                vectorClockService.mergeAndTick(response.getVectorClock());
            }
        }

        if (!receivedAnswerFromHigherNode) {
            // No higher node responded or responded with ANSWER -> Local node wins election!
            becomeCoordinator("Won election: No higher active nodes in cluster " + nodeConfig.getClusterId());
        }

        electionInProgress.set(false);
    }

    public synchronized Message handleIncomingMessage(Message incoming) {
        if (incoming == null) return null;

        MessageType type = incoming.getMessageType();
        int myPriority = getNodePriority(nodeConfig.getNodeId());
        int senderPriority = getNodePriority(incoming.getSenderId());

        VectorClockData currentVc = vectorClockService.mergeAndTick(incoming.getVectorClock());

        Message response = new Message();
        response.setSenderId(nodeConfig.getNodeId());
        response.setReceiverId(incoming.getSenderId());
        response.setClusterId(nodeConfig.getClusterId());
        response.setVectorClock(currentVc);

        if (type == MessageType.ELECTION) {
            log.info("Node {} received ELECTION from {}", nodeConfig.getNodeId(), incoming.getSenderId());
            if (myPriority > senderPriority) {
                // Return ANSWER and then start our own election
                response.setMessageType(MessageType.ANSWER);
                response.setPayload("I am higher priority than you (" + myPriority + " > " + senderPriority + ")");

                // Asynchronously run election to take coordinator
                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                        startElection("Received ELECTION from lower node " + incoming.getSenderId());
                    } catch (InterruptedException ignored) {}
                }).start();

                return response;
            } else {
                // If lower or equal, acknowledge
                response.setMessageType(MessageType.ANSWER);
                response.setPayload("OK");
                return response;
            }
        } else if (type == MessageType.COORDINATOR) {
            log.info("Node {} received COORDINATOR announcement from {}", nodeConfig.getNodeId(), incoming.getSenderId());
            if (myPriority > senderPriority) {
                // Local node has higher priority: BULLY the sender and take over!
                log.warn("Bully Preemption: Node {} has higher priority than announced coordinator {}. Reclaiming leadership!",
                        nodeConfig.getNodeId(), incoming.getSenderId());
                new Thread(() -> becomeCoordinator("Bully Preemption: higher priority node recovered")).start();
            } else {
                // Accept new coordinator
                this.currentLeaderId = incoming.getSenderId();
                this.currentRole = NodeRole.SENSOR;
                log.info("Node {} updated coordinator to {}", nodeConfig.getNodeId(), currentLeaderId);
            }
            response.setMessageType(MessageType.HEARTBEAT_ACK);
            response.setPayload("Acknowledged coordinator " + currentLeaderId);
            return response;
        }

        return null;
    }

    public synchronized void becomeCoordinator(String reason) {
        this.currentRole = NodeRole.CLUSTER_HEAD;
        this.currentLeaderId = nodeConfig.getNodeId();
        VectorClockData vc = vectorClockService.tick();

        log.info("PROMOTED: Node {} is now CLUSTER_HEAD for cluster {} on port {}. Reason: {}",
                nodeConfig.getNodeId(), nodeConfig.getClusterId(), nodeConfig.getPort(), reason);

        // Notify peers in cluster
        for (String peerUrl : nodeConfig.getPeerUrls()) {
            Message coordMsg = new Message();
            coordMsg.setSenderId(nodeConfig.getNodeId());
            coordMsg.setReceiverId(CommunicationService.inferNodeIdFromUrl(peerUrl));
            coordMsg.setClusterId(nodeConfig.getClusterId());
            coordMsg.setMessageType(MessageType.COORDINATOR);
            coordMsg.setVectorClock(vc);
            coordMsg.setPayload("Node " + nodeConfig.getNodeId() + " is now CLUSTER_HEAD");
            communicationService.sendToPeer(peerUrl, coordMsg);
        }

        // Notify Central Server
        communicationService.notifyCentralClusterHead(
                nodeConfig.getClusterId(),
                nodeConfig.getNodeId(),
                nodeConfig.getPort()
        );

        communicationService.sendElectionEventToCentral(
                nodeConfig.getClusterId(),
                nodeConfig.getNodeId(),
                nodeConfig.getNodeId(),
                "LEADER_PROMOTED",
                reason
        );
    }

    public synchronized void onNodeRecovery() {
        int myPriority = getNodePriority(nodeConfig.getNodeId());
        int leaderPriority = getNodePriority(currentLeaderId);

        log.info("Node {} recovered. Checking if priority ({}) > current leader {} ({})",
                nodeConfig.getNodeId(), myPriority, currentLeaderId, leaderPriority);

        if (myPriority > leaderPriority || nodeConfig.isInitialLeader()) {
            becomeCoordinator("Node recovered with higher Bully priority");
        } else {
            // Ping cluster peer
            startElection("Node recovered and checking cluster coordinator");
        }
    }

    public NodeRole getCurrentRole() { return currentRole; }
    public void setCurrentRole(NodeRole currentRole) { this.currentRole = currentRole; }
    public String getCurrentLeaderId() { return currentLeaderId; }
    public boolean isClusterHead() { return currentRole == NodeRole.CLUSTER_HEAD; }
}
