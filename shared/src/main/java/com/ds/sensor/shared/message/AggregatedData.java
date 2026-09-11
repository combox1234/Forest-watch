package com.ds.sensor.shared.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AggregatedData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String clusterId;
    private String clusterHeadId;
    private double avgTemperature;
    private double minTemperature;
    private double maxTemperature;
    private double avgHumidity;
    private int activeSensors;
    private int sampleCount;
    private int eventCount;
    private VectorClockData vectorClock;
    private long timestamp;
    private List<String> reportingNodes = new ArrayList<>();

    public AggregatedData() {
        this.timestamp = System.currentTimeMillis();
    }

    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public String getClusterHeadId() { return clusterHeadId; }
    public void setClusterHeadId(String clusterHeadId) { this.clusterHeadId = clusterHeadId; }

    public double getAvgTemperature() { return avgTemperature; }
    public void setAvgTemperature(double avgTemperature) { this.avgTemperature = avgTemperature; }

    public double getMinTemperature() { return minTemperature; }
    public void setMinTemperature(double minTemperature) { this.minTemperature = minTemperature; }

    public double getMaxTemperature() { return maxTemperature; }
    public void setMaxTemperature(double maxTemperature) { this.maxTemperature = maxTemperature; }

    public double getAvgHumidity() { return avgHumidity; }
    public void setAvgHumidity(double avgHumidity) { this.avgHumidity = avgHumidity; }

    public int getActiveSensors() { return activeSensors; }
    public void setActiveSensors(int activeSensors) { this.activeSensors = activeSensors; }

    public int getActiveSensorsCount() { return activeSensors; }
    public void setActiveSensorsCount(int activeSensors) { this.activeSensors = activeSensors; }

    public int getSampleCount() { return sampleCount; }
    public void setSampleCount(int sampleCount) { this.sampleCount = sampleCount; }

    public int getEventCount() { return eventCount; }
    public void setEventCount(int eventCount) { this.eventCount = eventCount; }

    public int getMotionEventsCount() { return eventCount; }
    public void setMotionEventsCount(int eventCount) { this.eventCount = eventCount; }

    public VectorClockData getVectorClock() { return vectorClock; }
    public void setVectorClock(VectorClockData vectorClock) { this.vectorClock = vectorClock; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public List<String> getReportingNodes() { return reportingNodes; }
    public void setReportingNodes(List<String> reportingNodes) { this.reportingNodes = reportingNodes; }
}
