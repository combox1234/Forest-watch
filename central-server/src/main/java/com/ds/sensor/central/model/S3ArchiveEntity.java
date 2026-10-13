package com.ds.sensor.central.model;

import jakarta.persistence.*;

@Entity
@Table(name = "s3_archive_log")
public class S3ArchiveEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long timestamp;
    private String bucketName;
    private String objectKey;
    private String clusterId;
    private String clusterHeadId;
    private int sampleCount;
    private double avgTemperature;
    private String vectorClock;
    private String eTag;
    private long sizeBytes;
    private String status;

    @Column(length = 2000)
    private String payloadJson;

    public S3ArchiveEntity() {}

    public S3ArchiveEntity(String bucketName, String objectKey, String clusterId, String clusterHeadId,
                           int sampleCount, double avgTemperature, String vectorClock, String eTag,
                           long sizeBytes, String status, String payloadJson) {
        this.timestamp = System.currentTimeMillis();
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.clusterId = clusterId;
        this.clusterHeadId = clusterHeadId;
        this.sampleCount = sampleCount;
        this.avgTemperature = avgTemperature;
        this.vectorClock = vectorClock;
        this.eTag = eTag;
        this.sizeBytes = sizeBytes;
        this.status = status;
        this.payloadJson = payloadJson;
    }

    public Long getId() { return id; }
    public long getTimestamp() { return timestamp; }
    public String getBucketName() { return bucketName; }
    public String getObjectKey() { return objectKey; }
    public String getClusterId() { return clusterId; }
    public String getClusterHeadId() { return clusterHeadId; }
    public int getSampleCount() { return sampleCount; }
    public double getAvgTemperature() { return avgTemperature; }
    public String getVectorClock() { return vectorClock; }
    public String getETag() { return eTag; }
    public long getSizeBytes() { return sizeBytes; }
    public String getStatus() { return status; }
    public String getPayloadJson() { return payloadJson; }

    public void setStatus(String status) { this.status = status; }
}
