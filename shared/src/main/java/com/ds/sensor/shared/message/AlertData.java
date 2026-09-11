package com.ds.sensor.shared.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AlertData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String alertId;
    private String clusterId;
    private String eventType;
    private List<String> sourceNodes = new ArrayList<>();
    private String description;
    private String severity; // INFO, WARNING, CRITICAL
    private String status;   // TRIGGERED, DEDUPLICATED
    private VectorClockData vectorClock;
    private long timestamp;

    public AlertData() {
        this.timestamp = System.currentTimeMillis();
    }

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public List<String> getSourceNodes() { return sourceNodes; }
    public void setSourceNodes(List<String> sourceNodes) { this.sourceNodes = sourceNodes; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public VectorClockData getVectorClock() { return vectorClock; }
    public void setVectorClock(VectorClockData vectorClock) { this.vectorClock = vectorClock; }

    public String getAlertType() { return eventType; }
    public void setAlertType(String alertType) { this.eventType = alertType; }

    public String getSourceNodeId() {
        return (sourceNodes != null && !sourceNodes.isEmpty()) ? sourceNodes.get(0) : null;
    }
    public void setSourceNodeId(String sourceNodeId) {
        if (this.sourceNodes == null) this.sourceNodes = new ArrayList<>();
        if (!this.sourceNodes.contains(sourceNodeId)) this.sourceNodes.add(sourceNodeId);
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
