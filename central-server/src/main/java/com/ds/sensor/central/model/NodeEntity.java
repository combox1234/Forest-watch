package com.ds.sensor.central.model;

import com.ds.sensor.shared.model.NodeRole;
import com.ds.sensor.shared.model.NodeStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "nodes")
public class NodeEntity {

    @Id
    @Column(name = "node_id", length = 32)
    private String nodeId; // S1, S2, S3, S4

    @Column(name = "port")
    private int port; // 8101, 8102, 8103, 8104

    @Column(name = "cluster_id", length = 32)
    private String clusterId; // C1, C2

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32)
    private NodeStatus status; // ACTIVE, FAILED, SUSPECTED

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 32)
    private NodeRole role; // SENSOR, CLUSTER_HEAD

    @Column(name = "temperature")
    private double temperature = 24.0;

    @Column(name = "humidity")
    private double humidity = 55.0;

    @Column(name = "battery")
    private double battery = 98.0;

    @Column(name = "motion")
    private boolean motion = false;

    @Column(name = "vector_clock", length = 64)
    private String vectorClock = "[0, 0, 0, 0]";

    @Column(name = "last_heartbeat")
    private long lastHeartbeat;

    @Column(name = "created_at")
    private long createdAt;

    @Column(name = "updated_at")
    private long updatedAt;

    public NodeEntity() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public NodeEntity(String nodeId, int port, String clusterId, NodeStatus status, NodeRole role) {
        this.nodeId = nodeId;
        this.port = port;
        this.clusterId = clusterId;
        this.status = status;
        this.role = role;
        this.lastHeartbeat = System.currentTimeMillis();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public NodeStatus getStatus() { return status; }
    public void setStatus(NodeStatus status) { this.status = status; }

    public NodeRole getRole() { return role; }
    public void setRole(NodeRole role) { this.role = role; }

    public long getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(long lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }

    public double getBattery() { return battery; }
    public void setBattery(double battery) { this.battery = battery; }

    public boolean isMotion() { return motion; }
    public void setMotion(boolean motion) { this.motion = motion; }

    public String getVectorClock() { return vectorClock; }
    public void setVectorClock(String vectorClock) { this.vectorClock = vectorClock; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
