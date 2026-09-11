package com.ds.sensor.central.service;

import com.ds.sensor.central.model.*;
import com.ds.sensor.central.repository.*;
import com.ds.sensor.shared.message.AlertData;
import com.ds.sensor.shared.message.Message;
import com.ds.sensor.shared.message.SensorReadingData;
import com.ds.sensor.shared.model.NodeRole;
import com.ds.sensor.shared.model.NodeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Service
public class CentralService {

    private static final Logger log = LoggerFactory.getLogger(CentralService.class);

    private final NodeRepository nodeRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final EventRepository eventRepository;
    private final AlertRepository alertRepository;
    private final ClusterStateRepository clusterStateRepository;
    private final CommunicationLogRepository communicationLogRepository;
    private final ElectionLogRepository electionLogRepository;

    @Autowired
    public CentralService(NodeRepository nodeRepository,
                          SensorReadingRepository sensorReadingRepository,
                          EventRepository eventRepository,
                          AlertRepository alertRepository,
                          ClusterStateRepository clusterStateRepository,
                          CommunicationLogRepository communicationLogRepository,
                          ElectionLogRepository electionLogRepository) {
        this.nodeRepository = nodeRepository;
        this.sensorReadingRepository = sensorReadingRepository;
        this.eventRepository = eventRepository;
        this.alertRepository = alertRepository;
        this.clusterStateRepository = clusterStateRepository;
        this.communicationLogRepository = communicationLogRepository;
        this.electionLogRepository = electionLogRepository;
    }

    @PostConstruct
    public void initDefaultTopology() {
        // Initialize default cluster states if not present
        if (!clusterStateRepository.existsById("C1")) {
            clusterStateRepository.save(new ClusterStateEntity("C1", "Alpha Forest Canopy", "S2", 8102, 2));
        }
        if (!clusterStateRepository.existsById("C2")) {
            clusterStateRepository.save(new ClusterStateEntity("C2", "Beta River Basin", "S4", 8104, 2));
        }

        // Initialize 4 sensor nodes in the database with their default configurations
        registerOrUpdateNode("S1", 8101, "C1", NodeStatus.ACTIVE, NodeRole.SENSOR);
        registerOrUpdateNode("S2", 8102, "C1", NodeStatus.ACTIVE, NodeRole.CLUSTER_HEAD);
        registerOrUpdateNode("S3", 8103, "C2", NodeStatus.ACTIVE, NodeRole.SENSOR);
        registerOrUpdateNode("S4", 8104, "C2", NodeStatus.ACTIVE, NodeRole.CLUSTER_HEAD);

        log.info("Central Server topology initialized: S1/S2 in C1, S3/S4 in C2.");
    }

    @Transactional
    public NodeEntity registerOrUpdateNode(String nodeId, int port, String clusterId, NodeStatus status, NodeRole role) {
        Optional<NodeEntity> existing = nodeRepository.findById(nodeId);
        NodeEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            if (port > 0) entity.setPort(port);
            if (clusterId != null) entity.setClusterId(clusterId);
            if (status != null) entity.setStatus(status);
            if (role != null) entity.setRole(role);
            entity.setLastHeartbeat(System.currentTimeMillis());
            entity.setUpdatedAt(System.currentTimeMillis());
        } else {
            entity = new NodeEntity(nodeId, port, clusterId, status != null ? status : NodeStatus.ACTIVE, role != null ? role : NodeRole.SENSOR);
        }
        return nodeRepository.save(entity);
    }

    @Transactional
    public void recordHeartbeat(String nodeId) {
        recordHeartbeat(nodeId, null, null, null, null, null);
    }

    @Transactional
    public void recordHeartbeat(String nodeId, Double temp, Double hum, Double batt, Boolean motion, String vc) {
        nodeRepository.findById(nodeId).ifPresent(node -> {
            node.setStatus(NodeStatus.ACTIVE);
            node.setLastHeartbeat(System.currentTimeMillis());
            if (temp != null) node.setTemperature(temp);
            if (hum != null) node.setHumidity(hum);
            if (batt != null) node.setBattery(batt);
            if (motion != null) node.setMotion(motion);
            if (vc != null) node.setVectorClock(vc);
            node.setUpdatedAt(System.currentTimeMillis());
            nodeRepository.save(node);
        });
    }

    @Transactional
    public void updateNodeStatus(String nodeId, NodeStatus status) {
        nodeRepository.findById(nodeId).ifPresent(node -> {
            node.setStatus(status);
            node.setUpdatedAt(System.currentTimeMillis());
            nodeRepository.save(node);
        });
    }

    @Transactional
    public void updateClusterHead(String clusterId, String newHeadId, int port) {
        log.info("Updating cluster head for cluster {}: new head is {}", clusterId, newHeadId);

        // Update all nodes in this cluster
        List<NodeEntity> nodes = nodeRepository.findByClusterId(clusterId);
        for (NodeEntity n : nodes) {
            if (n.getNodeId().equalsIgnoreCase(newHeadId)) {
                n.setRole(NodeRole.CLUSTER_HEAD);
                n.setStatus(NodeStatus.ACTIVE);
                n.setLastHeartbeat(System.currentTimeMillis());
            } else {
                n.setRole(NodeRole.SENSOR);
            }
            n.setUpdatedAt(System.currentTimeMillis());
            nodeRepository.save(n);
        }

        // Update cluster state entity
        clusterStateRepository.findById(clusterId).ifPresent(c -> {
            c.setCurrentHeadId(newHeadId);
            c.setHeadPort(port);
            c.setLastUpdated(System.currentTimeMillis());
            clusterStateRepository.save(c);
        });

        // Record in election log
        electionLogRepository.save(new ElectionLogEntity(clusterId, newHeadId, newHeadId, "LEADER_PROMOTED",
                "Node " + newHeadId + " became Cluster Head on port " + port));
    }

    @Transactional
    public void recordElectionEvent(String clusterId, String initiatorId, String leaderId, String eventType, String details) {
        electionLogRepository.save(new ElectionLogEntity(clusterId, initiatorId, leaderId, eventType, details));
    }

    @Transactional
    public void recordCommunicationLog(Message message) {
        if (message == null) return;
        String vcStr = message.getVectorClock() != null ? message.getVectorClock().toString() : "[]";
        String payload = message.getPayload() != null ? message.getPayload().toString() : "";
        if (payload.length() > 500) {
            payload = payload.substring(0, 500) + "...";
        }
        String receiver = message.getReceiverId();
        if (receiver == null || receiver.trim().isEmpty() || "null".equalsIgnoreCase(receiver)) {
            receiver = (message.getClusterId() != null && !message.getClusterId().trim().isEmpty()) 
                    ? message.getClusterId() + " (CLUSTER)" 
                    : "CENTRAL";
        }
        communicationLogRepository.save(new CommunicationLogEntity(
                message.getSenderId(),
                receiver,
                message.getClusterId(),
                message.getMessageType() != null ? message.getMessageType().name() : "UNKNOWN",
                vcStr,
                payload
        ));
    }

    @Transactional
    public SensorReadingEntity recordRawReading(SensorReadingData data) {
        if (data == null) return null;
        SensorReadingEntity entity = new SensorReadingEntity(
                data.getNodeId(),
                data.getClusterId(),
                data.getTemperature(),
                data.getHumidity(),
                data.getBatteryLevel(),
                data.isMotionDetected(),
                data.getVectorClock() != null ? data.getVectorClock().toString() : "[]"
        );
        return sensorReadingRepository.save(entity);
    }

    @Transactional
    public EventEntity recordEvent(String eventType, String clusterId, String sourceNodeId, String description, String vectorClock) {
        EventEntity event = new EventEntity(eventType, clusterId, sourceNodeId, description, vectorClock);
        return eventRepository.save(event);
    }

    @Transactional
    public AlertEntity recordAlert(AlertData alertData) {
        if (alertData == null) return null;

        // Check for deduplication: if identical alert type in same cluster was received in last 10 seconds
        long recentWindow = System.currentTimeMillis() - 10000;
        List<AlertEntity> recent = alertRepository.findByClusterIdOrderByTimestampDesc(alertData.getClusterId());
        for (AlertEntity a : recent) {
            if (a.getTimestamp() >= recentWindow && a.getAlertType().equalsIgnoreCase(alertData.getAlertType())) {
                // Merge source node if not already present
                if (!a.getSourceNodes().contains(alertData.getSourceNodeId())) {
                    a.getSourceNodes().add(alertData.getSourceNodeId());
                    alertRepository.save(a);
                    log.info("Deduplicated alert {} from node {}. Merged into existing alert id={}", alertData.getAlertType(), alertData.getSourceNodeId(), a.getId());
                }
                return a;
            }
        }

        List<String> sources = new ArrayList<>();
        if (alertData.getSourceNodeId() != null) {
            sources.add(alertData.getSourceNodeId());
        }
        String vcStr = alertData.getVectorClock() != null ? alertData.getVectorClock().toString() : "[]";
        AlertEntity newAlert = new AlertEntity(alertData.getAlertType(), alertData.getClusterId(), alertData.getDescription(), vcStr, sources);
        return alertRepository.save(newAlert);
    }

    @Scheduled(fixedRate = 1500)
    @Transactional
    public void monitorLiveness() {
        long now = System.currentTimeMillis();
        List<NodeEntity> nodes = nodeRepository.findAll();
        for (NodeEntity node : nodes) {
            long diff = now - node.getLastHeartbeat();
            if (diff > 4500 && node.getStatus() == NodeStatus.ACTIVE) {
                node.setStatus(NodeStatus.SUSPECTED);
                nodeRepository.save(node);
                log.warn("Node {} is SUSPECTED (last heartbeat {}s ago)", node.getNodeId(), diff / 1000);
            } else if (diff > 7500 && (node.getStatus() == NodeStatus.SUSPECTED || node.getStatus() == NodeStatus.ACTIVE)) {
                node.setStatus(NodeStatus.FAILED);
                nodeRepository.save(node);
                log.error("Node {} marked as FAILED (no heartbeat for {}s)", node.getNodeId(), diff / 1000);
            }
        }

        // Update active member counts in cluster states
        for (ClusterStateEntity cs : clusterStateRepository.findAll()) {
            int active = 0;
            List<NodeEntity> clusterNodes = nodeRepository.findByClusterId(cs.getClusterId());
            for (NodeEntity cn : clusterNodes) {
                if (cn.getStatus() == NodeStatus.ACTIVE) {
                    active++;
                }
            }
            cs.setActiveMembers(active);
            clusterStateRepository.save(cs);
        }
    }

    public List<NodeEntity> getAllNodes() { return nodeRepository.findAll(); }
    public List<ClusterStateEntity> getAllClusters() { return clusterStateRepository.findAll(); }
    public List<CommunicationLogEntity> getRecentLogs() { return communicationLogRepository.findTop100ByOrderByTimestampDesc(); }
    public List<EventEntity> getRecentEvents() { return eventRepository.findTop50ByOrderByTimestampDesc(); }
    public List<AlertEntity> getRecentAlerts() { return alertRepository.findTop50ByOrderByTimestampDesc(); }
    public List<ElectionLogEntity> getRecentElectionLogs() { return electionLogRepository.findTop50ByOrderByTimestampDesc(); }
}
