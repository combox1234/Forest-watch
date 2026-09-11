package com.ds.sensor.shared.message;

import java.io.Serializable;
import java.util.UUID;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private String messageId;
    private String senderId;
    private String receiverId;
    private String clusterId;
    private MessageType messageType;
    private long timestamp;
    private VectorClockData vectorClock;
    private Object payload;

    public Message() {
        this.messageId = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }

    public Message(String senderId, String receiverId, String clusterId, MessageType messageType,
                   VectorClockData vectorClock, Object payload) {
        this.messageId = UUID.randomUUID().toString();
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.clusterId = clusterId;
        this.messageType = messageType;
        this.vectorClock = vectorClock;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public VectorClockData getVectorClock() { return vectorClock; }
    public void setVectorClock(VectorClockData vectorClock) { this.vectorClock = vectorClock; }

    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }

    @Override
    public String toString() {
        return String.format("[%s] %s -> %s (%s) VC=%s",
                messageType, senderId, receiverId, clusterId, vectorClock != null ? vectorClock.toString() : "[]");
    }
}
