package com.ds.sensor.central.model;

import jakarta.persistence.*;

@Entity
@Table(name = "lambda_invocation_log")
public class LambdaInvocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long timestamp;
    private String requestId;
    private String functionName;
    private String triggerEvent;
    private String clusterId;
    private String sourceNodeId;
    private long durationMs;
    private int statusCode;
    private String vectorClock;

    @Column(length = 2000)
    private String responsePayload;

    public LambdaInvocationEntity() {}

    public LambdaInvocationEntity(String requestId, String functionName, String triggerEvent,
                                  String clusterId, String sourceNodeId, long durationMs,
                                  int statusCode, String vectorClock, String responsePayload) {
        this.timestamp = System.currentTimeMillis();
        this.requestId = requestId;
        this.functionName = functionName;
        this.triggerEvent = triggerEvent;
        this.clusterId = clusterId;
        this.sourceNodeId = sourceNodeId;
        this.durationMs = durationMs;
        this.statusCode = statusCode;
        this.vectorClock = vectorClock;
        this.responsePayload = responsePayload;
    }

    public Long getId() { return id; }
    public long getTimestamp() { return timestamp; }
    public String getRequestId() { return requestId; }
    public String getFunctionName() { return functionName; }
    public String getTriggerEvent() { return triggerEvent; }
    public String getClusterId() { return clusterId; }
    public String getSourceNodeId() { return sourceNodeId; }
    public long getDurationMs() { return durationMs; }
    public int getStatusCode() { return statusCode; }
    public String getVectorClock() { return vectorClock; }
    public String getResponsePayload() { return responsePayload; }
}
