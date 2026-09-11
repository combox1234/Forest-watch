package com.ds.sensor.central.model;

import jakarta.persistence.*;

@Entity
@Table(name = "election_logs")
public class ElectionLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cluster_id", length = 32)
    private String clusterId;

    @Column(name = "initiator_id", length = 32)
    private String initiatorId;

    @Column(name = "elected_leader_id", length = 32)
    private String electedLeaderId;

    @Column(name = "event_type", length = 64)
    private String eventType; // ELECTION_STARTED, LEADER_PROMOTED, LEADER_BULLIED, FAILOVER

    @Column(name = "details", length = 512)
    private String details;

    @Column(name = "timestamp")
    private long timestamp;

    public ElectionLogEntity() {
        this.timestamp = System.currentTimeMillis();
    }

    public ElectionLogEntity(String clusterId, String initiatorId, String electedLeaderId, String eventType, String details) {
        this.clusterId = clusterId;
        this.initiatorId = initiatorId;
        this.electedLeaderId = electedLeaderId;
        this.eventType = eventType;
        this.details = details;
        this.timestamp = System.currentTimeMillis();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public String getInitiatorId() { return initiatorId; }
    public void setInitiatorId(String initiatorId) { this.initiatorId = initiatorId; }

    public String getElectedLeaderId() { return electedLeaderId; }
    public void setElectedLeaderId(String electedLeaderId) { this.electedLeaderId = electedLeaderId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
