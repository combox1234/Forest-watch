package com.ds.sensor.model;

import jakarta.persistence.*;

@Entity
@Table(name = "alert")
public class AlertRecord {

    @Id
    @Column(name = "alert_id", length = 64)
    private String alertId;

    @Column(name = "cluster_id", length = 64, nullable = false)
    private String clusterId;

    @Column(name = "event_type", length = 64)
    private String eventType; // WILDFIRE_TEMP, FLOOD_WATER, INTRUSION_MOTION, etc.

    @Column(name = "severity", length = 32)
    private String severity; // INFO, WARNING, CRITICAL

    @Column(name = "source_sensor", length = 64)
    private String sourceSensor;

    @Column(name = "event_time")
    private Long eventTime;

    @Column(name = "status", length = 32)
    private String status; // TRIGGERED, DEDUPLICATED_MUTEX, RESOLVED

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "is_deduplicated")
    private Boolean isDeduplicated = false;

    @Column(name = "vector_clock_json", length = 1024)
    private String vectorClockJson;

    public AlertRecord() {}

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getSourceSensor() { return sourceSensor; }
    public void setSourceSensor(String sourceSensor) { this.sourceSensor = sourceSensor; }

    public Long getEventTime() { return eventTime; }
    public void setEventTime(Long eventTime) { this.eventTime = eventTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsDeduplicated() { return isDeduplicated; }
    public void setIsDeduplicated(Boolean isDeduplicated) { this.isDeduplicated = isDeduplicated; }

    public String getVectorClockJson() { return vectorClockJson; }
    public void setVectorClockJson(String vectorClockJson) { this.vectorClockJson = vectorClockJson; }
}
