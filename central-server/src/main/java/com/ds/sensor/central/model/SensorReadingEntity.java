package com.ds.sensor.central.model;

import jakarta.persistence.*;

@Entity
@Table(name = "sensor_readings")
public class SensorReadingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_id", length = 32)
    private String nodeId;

    @Column(name = "cluster_id", length = 32)
    private String clusterId;

    private double temperature;
    private double humidity;
    private boolean motion;
    private double battery;
    private double connectivity;

    @Column(name = "vector_clock", length = 64)
    private String vectorClock;

    private long timestamp;

    public SensorReadingEntity() {
        this.timestamp = System.currentTimeMillis();
    }

    public SensorReadingEntity(String nodeId, String clusterId, double temperature, double humidity,
                               double battery, boolean motion, String vectorClock) {
        this.nodeId = nodeId;
        this.clusterId = clusterId;
        this.temperature = temperature;
        this.humidity = humidity;
        this.battery = battery;
        this.motion = motion;
        this.vectorClock = vectorClock;
        this.connectivity = 100.0;
        this.timestamp = System.currentTimeMillis();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }

    public boolean isMotion() { return motion; }
    public void setMotion(boolean motion) { this.motion = motion; }

    public double getBattery() { return battery; }
    public void setBattery(double battery) { this.battery = battery; }

    public double getConnectivity() { return connectivity; }
    public void setConnectivity(double connectivity) { this.connectivity = connectivity; }

    public String getVectorClock() { return vectorClock; }
    public void setVectorClock(String vectorClock) { this.vectorClock = vectorClock; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
