package com.ds.sensor.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sensor")
public class SensorNodeEntity {

    @Id
    @Column(name = "sensor_id", length = 64)
    private String sensorId;

    @Column(name = "cluster_id", length = 64, nullable = false)
    private String clusterId;

    @Column(name = "type", length = 64)
    private String type; // TEMPERATURE, HUMIDITY, WATER_QUALITY, MOTION

    @Column(name = "status", length = 32)
    private String status; // HEALTHY, SUSPECTED, FAILED

    @Column(name = "battery_score")
    private Double batteryScore;

    @Column(name = "last_heartbeat")
    private Long lastHeartbeat;

    @Column(name = "vector_clock_json", length = 1024)
    private String vectorClockJson;

    public SensorNodeEntity() {}

    public SensorNodeEntity(String sensorId, String clusterId, String type, String status, Double batteryScore) {
        this.sensorId = sensorId;
        this.clusterId = clusterId;
        this.type = type;
        this.status = status;
        this.batteryScore = batteryScore;
        this.lastHeartbeat = System.currentTimeMillis();
    }

    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }

    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getBatteryScore() { return batteryScore; }
    public void setBatteryScore(Double batteryScore) { this.batteryScore = batteryScore; }

    public Long getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(Long lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

    public String getVectorClockJson() { return vectorClockJson; }
    public void setVectorClockJson(String vectorClockJson) { this.vectorClockJson = vectorClockJson; }
}
