package com.ds.sensor.central.model;

import jakarta.persistence.*;

@Entity
@Table(name = "communication_logs")
public class CommunicationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", length = 32)
    private String senderId;

    @Column(name = "receiver_id", length = 32)
    private String receiverId;

    @Column(name = "cluster_id", length = 32)
    private String clusterId;

    @Column(name = "message_type", length = 64)
    private String messageType;

    @Column(name = "vector_clock", length = 128)
    private String vectorClock;

    @Column(name = "payload_summary", length = 1024)
    private String payloadSummary;

    @Column(name = "timestamp")
    private long timestamp;

    public CommunicationLogEntity() {
        this.timestamp = System.currentTimeMillis();
    }

    public CommunicationLogEntity(String senderId, String receiverId, String clusterId, String messageType, String vectorClock, String payloadSummary) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.clusterId = clusterId;
        this.messageType = messageType;
        this.vectorClock = vectorClock;
        this.payloadSummary = payloadSummary;
        this.timestamp = System.currentTimeMillis();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public String getVectorClock() { return vectorClock; }
    public void setVectorClock(String vectorClock) { this.vectorClock = vectorClock; }

    public String getPayloadSummary() { return payloadSummary; }
    public void setPayloadSummary(String payloadSummary) { this.payloadSummary = payloadSummary; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
