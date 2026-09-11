package com.ds.sensor.central.model;

import jakarta.persistence.*;

@Entity
@Table(name = "events")
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_id", length = 32)
    private String nodeId;

    @Column(name = "cluster_id", length = 32)
    private String clusterId;

    @Column(name = "event_type", length = 64)
    private String eventType; // ANIMAL_DETECTED, HIGH_TEMPERATURE, MOTION_DETECTED, LOW_BATTERY

    @Column(name = "payload", length = 512)
    private String payload;

    @Column(name = "vector_clock", length = 64)
    private String vectorClock;

    private long timestamp;

    public EventEntity() {
        this.timestamp = System.currentTimeMillis();
    }

    public EventEntity(String nodeId, String clusterId, String eventType, String payload, String vectorClock) {
        this.nodeId = nodeId;
        this.clusterId = clusterId;
        this.eventType = eventType;
        this.payload = payload;
        this.vectorClock = vectorClock;
        this.timestamp = System.currentTimeMillis();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getVectorClock() { return vectorClock; }
    public void setVectorClock(String vectorClock) { this.vectorClock = vectorClock; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
