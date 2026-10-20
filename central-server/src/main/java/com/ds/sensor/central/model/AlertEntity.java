package com.ds.sensor.central.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "alerts")
public class AlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_type", length = 64)
    private String alertType; // e.g. WILDFIRE_RISK, CRITICAL_HEAT, MOTION_INTRUSION

    @Column(name = "cluster_id", length = 32)
    private String clusterId;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "vector_clock", length = 128)
    private String vectorClock;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "alert_source_nodes", joinColumns = @JoinColumn(name = "alert_id"))
    @Column(name = "source_node")
    private List<String> sourceNodes = new ArrayList<>();

    @Column(name = "timestamp")
    private long timestamp;

    public AlertEntity() {
        this.timestamp = System.currentTimeMillis();
    }

    public AlertEntity(String alertType, String clusterId, String description, String vectorClock, List<String> sourceNodes) {
        this.alertType = alertType;
        this.clusterId = clusterId;
        this.description = description;
        this.vectorClock = vectorClock;
        if (sourceNodes != null) {
            this.sourceNodes = new ArrayList<>(sourceNodes);
        }
        this.timestamp = System.currentTimeMillis();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }

    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVectorClock() { return vectorClock; }
    public void setVectorClock(String vectorClock) { this.vectorClock = vectorClock; }

    public List<String> getSourceNodes() { return sourceNodes; }
    public void setSourceNodes(List<String> sourceNodes) { this.sourceNodes = sourceNodes; }

    @Column(name = "acknowledged")
    private boolean acknowledged = false;

    @Column(name = "acknowledged_by", length = 64)
    private String acknowledgedBy;

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isAcknowledged() { return acknowledged; }
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }

    public String getAcknowledgedBy() { return acknowledgedBy; }
    public void setAcknowledgedBy(String acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }
}
