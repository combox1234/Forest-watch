package com.ds.sensor.shared.message;

import java.io.Serializable;

public class SensorReadingData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nodeId;
    private String clusterId;
    private double temperature;
    private double humidity;
    private boolean motion;
    private double battery;
    private double connectivity;
    private long timestamp;
    private VectorClockData vectorClock;

    public SensorReadingData() {
        this.timestamp = System.currentTimeMillis();
    }

    public SensorReadingData(String nodeId, String clusterId, double temperature, double humidity,
                             boolean motion, double battery, double connectivity, VectorClockData vectorClock) {
        this.nodeId = nodeId;
        this.clusterId = clusterId;
        this.temperature = temperature;
        this.humidity = humidity;
        this.motion = motion;
        this.battery = battery;
        this.connectivity = connectivity;
        this.vectorClock = vectorClock;
        this.timestamp = System.currentTimeMillis();
    }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }

    public boolean isMotion() { return motion; }
    public void setMotion(boolean motion) { this.motion = motion; }

    public double getBattery() { return battery; }
    public void setBattery(double battery) { this.battery = battery; }

    public double getConnectivity() { return connectivity; }
    public void setConnectivity(double connectivity) { this.connectivity = connectivity; }

    public boolean isMotionDetected() { return motion; }
    public void setMotionDetected(boolean motion) { this.motion = motion; }

    public double getBatteryLevel() { return battery; }
    public void setBatteryLevel(double battery) { this.battery = battery; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public VectorClockData getVectorClock() { return vectorClock; }
    public void setVectorClock(VectorClockData vectorClock) { this.vectorClock = vectorClock; }
}
