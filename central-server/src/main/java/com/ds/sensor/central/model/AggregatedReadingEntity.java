package com.ds.sensor.central.model;

import jakarta.persistence.*;

@Entity
@Table(name = "aggregated_readings")
public class AggregatedReadingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cluster_id", length = 32)
    private String clusterId;

    @Column(name = "cluster_head_id", length = 32)
    private String clusterHeadId;

    @Column(name = "sample_count")
    private int sampleCount;

    @Column(name = "avg_temperature")
    private double avgTemperature;

    @Column(name = "min_temperature")
    private double minTemperature;

    @Column(name = "max_temperature")
    private double maxTemperature;

    @Column(name = "avg_humidity")
    private double avgHumidity;

    @Column(name = "active_sensors_count")
    private int activeSensorsCount;

    @Column(name = "motion_events_count")
    private int motionEventsCount;

    @Column(name = "vector_clock", length = 128)
    private String vectorClock;

    @Column(name = "timestamp")
    private long timestamp;

    public AggregatedReadingEntity() {
        this.timestamp = System.currentTimeMillis();
    }

    public AggregatedReadingEntity(String clusterId, String clusterHeadId, int sampleCount, double avgTemperature,
                                   double minTemperature, double maxTemperature, double avgHumidity,
                                   int activeSensorsCount, int motionEventsCount, String vectorClock) {
        this.clusterId = clusterId;
        this.clusterHeadId = clusterHeadId;
        this.sampleCount = sampleCount;
        this.avgTemperature = avgTemperature;
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
        this.avgHumidity = avgHumidity;
        this.activeSensorsCount = activeSensorsCount;
        this.motionEventsCount = motionEventsCount;
        this.vectorClock = vectorClock;
        this.timestamp = System.currentTimeMillis();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public String getClusterHeadId() { return clusterHeadId; }
    public void setClusterHeadId(String clusterHeadId) { this.clusterHeadId = clusterHeadId; }

    public int getSampleCount() { return sampleCount; }
    public void setSampleCount(int sampleCount) { this.sampleCount = sampleCount; }

    public double getAvgTemperature() { return avgTemperature; }
    public void setAvgTemperature(double avgTemperature) { this.avgTemperature = avgTemperature; }

    public double getMinTemperature() { return minTemperature; }
    public void setMinTemperature(double minTemperature) { this.minTemperature = minTemperature; }

    public double getMaxTemperature() { return maxTemperature; }
    public void setMaxTemperature(double maxTemperature) { this.maxTemperature = maxTemperature; }

    public double getAvgHumidity() { return avgHumidity; }
    public void setAvgHumidity(double avgHumidity) { this.avgHumidity = avgHumidity; }

    public int getActiveSensorsCount() { return activeSensorsCount; }
    public void setActiveSensorsCount(int activeSensorsCount) { this.activeSensorsCount = activeSensorsCount; }

    public int getMotionEventsCount() { return motionEventsCount; }
    public void setMotionEventsCount(int motionEventsCount) { this.motionEventsCount = motionEventsCount; }

    public String getVectorClock() { return vectorClock; }
    public void setVectorClock(String vectorClock) { this.vectorClock = vectorClock; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
