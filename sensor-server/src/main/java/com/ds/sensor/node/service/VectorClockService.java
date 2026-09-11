package com.ds.sensor.node.service;

import com.ds.sensor.node.config.NodeConfig;
import com.ds.sensor.shared.message.VectorClockData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VectorClockService {

    private final NodeConfig nodeConfig;
    private final VectorClockData localClock;

    @Autowired
    public VectorClockService(NodeConfig nodeConfig) {
        this.nodeConfig = nodeConfig;
        this.localClock = new VectorClockData();
    }

    public int getLocalNodeIndex() {
        String id = nodeConfig.getNodeId();
        if (id == null) return 0;
        return switch (id.trim().toUpperCase()) {
            case "S1" -> 0;
            case "S2" -> 1;
            case "S3" -> 2;
            case "S4" -> 3;
            default -> 0;
        };
    }

    public synchronized VectorClockData tick() {
        localClock.increment(getLocalNodeIndex());
        return localClock.copy();
    }

    public synchronized VectorClockData mergeAndTick(VectorClockData incoming) {
        if (incoming != null) {
            localClock.merge(incoming);
        }
        localClock.increment(getLocalNodeIndex());
        return localClock.copy();
    }

    public synchronized VectorClockData getCurrentClock() {
        return localClock.copy();
    }

    public String getCurrentClockString() {
        return localClock.toString();
    }

    public String compare(VectorClockData c1, VectorClockData c2) {
        if (c1 == null || c2 == null) return "UNKNOWN";
        if (c1.happensBefore(c2)) return "HAPPENS_BEFORE";
        if (c2.happensBefore(c1)) return "HAPPENS_AFTER";
        if (c1.isConcurrent(c2)) return "CONCURRENT";
        return "EQUAL";
    }
}
