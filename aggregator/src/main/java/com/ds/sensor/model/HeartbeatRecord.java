package com.ds.sensor.model;

import jakarta.persistence.*;

@Entity
@Table(name = "heartbeat_log")
public class HeartbeatRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "heartbeat_id")
    private Long heartbeatId;

    @Column(name = "sensor_id", length = 64, nullable = false)
    private String sensorId;

    @Column(name = "cluster_id", length = 64)
    private String clusterId;

    @Column(name = "received_at")
    private Long receivedAt;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "status", length = 32)
    private String status; // HEALTHY, SUSPECTED, FAILED

    public HeartbeatRecord() {}

    public HeartbeatRecord(String sensorId, String clusterId, Long receivedAt, Long latencyMs, String status) {
        this.sensorId = sensorId;
        this.clusterId = clusterId;
        this.receivedAt = receivedAt;
        this.latencyMs = latencyMs;
        this.status = status;
    }

    public Long getHeartbeatId() { return heartbeatId; }
    public void setHeartbeatId(Long heartbeatId) { this.heartbeatId = heartbeatId; }

    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }

    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public Long getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Long receivedAt) { this.receivedAt = receivedAt; }

    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
