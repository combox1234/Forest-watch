package com.ds.sensor.model;

import jakarta.persistence.*;

@Entity
@Table(name = "reading")
public class ReadingRecord {

    @Id
    @Column(name = "reading_id", length = 64)
    private String readingId;

    @Column(name = "cluster_id", length = 64, nullable = false)
    private String clusterId;

    @Column(name = "leader_id", length = 64)
    private String leaderId;

    @Column(name = "timestamp")
    private Long timestamp;

    @Column(name = "sample_count")
    private Integer sampleCount;

    @Column(name = "avg_temperature")
    private Double avgTemperature;

    @Column(name = "max_temperature")
    private Double maxTemperature;

    @Column(name = "min_temperature")
    private Double minTemperature;

    @Column(name = "avg_humidity")
    private Double avgHumidity;

    @Column(name = "avg_water_ph")
    private Double avgWaterPh;

    @Column(name = "avg_water_turbidity")
    private Double avgWaterTurbidity;

    @Column(name = "motion_event_count")
    private Integer motionEventCount;

    @Column(name = "vector_clock_json", length = 1024)
    private String vectorClockJson;

    public ReadingRecord() {}

    public String getReadingId() { return readingId; }
    public void setReadingId(String readingId) { this.readingId = readingId; }

    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public String getLeaderId() { return leaderId; }
    public void setLeaderId(String leaderId) { this.leaderId = leaderId; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public Integer getSampleCount() { return sampleCount; }
    public void setSampleCount(Integer sampleCount) { this.sampleCount = sampleCount; }

    public Double getAvgTemperature() { return avgTemperature; }
    public void setAvgTemperature(Double avgTemperature) { this.avgTemperature = avgTemperature; }

    public Double getMaxTemperature() { return maxTemperature; }
    public void setMaxTemperature(Double maxTemperature) { this.maxTemperature = maxTemperature; }

    public Double getMinTemperature() { return minTemperature; }
    public void setMinTemperature(Double minTemperature) { this.minTemperature = minTemperature; }

    public Double getAvgHumidity() { return avgHumidity; }
    public void setAvgHumidity(Double avgHumidity) { this.avgHumidity = avgHumidity; }

    public Double getAvgWaterPh() { return avgWaterPh; }
    public void setAvgWaterPh(Double avgWaterPh) { this.avgWaterPh = avgWaterPh; }

    public Double getAvgWaterTurbidity() { return avgWaterTurbidity; }
    public void setAvgWaterTurbidity(Double avgWaterTurbidity) { this.avgWaterTurbidity = avgWaterTurbidity; }

    public Integer getMotionEventCount() { return motionEventCount; }
    public void setMotionEventCount(Integer motionEventCount) { this.motionEventCount = motionEventCount; }

    public String getVectorClockJson() { return vectorClockJson; }
    public void setVectorClockJson(String vectorClockJson) { this.vectorClockJson = vectorClockJson; }
}
