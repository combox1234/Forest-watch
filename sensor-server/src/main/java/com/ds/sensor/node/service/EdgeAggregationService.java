package com.ds.sensor.node.service;

import com.ds.sensor.node.config.NodeConfig;
import com.ds.sensor.shared.message.AggregatedData;
import com.ds.sensor.shared.message.SensorReadingData;
import com.ds.sensor.shared.message.VectorClockData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class EdgeAggregationService {

    private static final Logger log = LoggerFactory.getLogger(EdgeAggregationService.class);

    private final NodeConfig nodeConfig;
    private final CommunicationService communicationService;
    private final VectorClockService vectorClockService;

    private final Queue<SensorReadingData> readingBuffer = new ConcurrentLinkedQueue<>();
    private final List<AggregatedData> history = new ArrayList<>();

    @Autowired
    public EdgeAggregationService(NodeConfig nodeConfig,
                                  CommunicationService communicationService,
                                  VectorClockService vectorClockService) {
        this.nodeConfig = nodeConfig;
        this.communicationService = communicationService;
        this.vectorClockService = vectorClockService;
    }

    public void recordReading(SensorReadingData reading) {
        if (reading != null) {
            readingBuffer.add(reading);
            while (readingBuffer.size() > 100) {
                readingBuffer.poll();
            }
        }
    }

    public synchronized AggregatedData computeAndSendAggregation(SensorReadingData fallbackReading) {
        List<SensorReadingData> samples = new ArrayList<>();
        while (!readingBuffer.isEmpty()) {
            samples.add(readingBuffer.poll());
        }

        if (samples.isEmpty() && fallbackReading != null) {
            samples.add(fallbackReading);
        }

        if (samples.isEmpty()) {
            log.info("No readings to aggregate for cluster {}", nodeConfig.getClusterId());
            return null;
        }

        double sumTemp = 0.0;
        double minTemp = Double.MAX_VALUE;
        double maxTemp = Double.MIN_VALUE;
        double sumHum = 0.0;
        int motionEvents = 0;
        Set<String> uniqueNodes = new HashSet<>();

        for (SensorReadingData r : samples) {
            sumTemp += r.getTemperature();
            minTemp = Math.min(minTemp, r.getTemperature());
            maxTemp = Math.max(maxTemp, r.getTemperature());
            sumHum += r.getHumidity();
            if (r.isMotion()) motionEvents++;
            if (r.getNodeId() != null) uniqueNodes.add(r.getNodeId());
        }

        int count = samples.size();
        double avgTemp = Math.round((sumTemp / count) * 10.0) / 10.0;
        double avgHum = Math.round((sumHum / count) * 10.0) / 10.0;
        minTemp = Math.round(minTemp * 10.0) / 10.0;
        maxTemp = Math.round(maxTemp * 10.0) / 10.0;

        VectorClockData vc = vectorClockService.getCurrentClock();

        AggregatedData agg = new AggregatedData();
        agg.setClusterId(nodeConfig.getClusterId());
        agg.setClusterHeadId(nodeConfig.getNodeId());
        agg.setSampleCount(count);
        agg.setAvgTemperature(avgTemp);
        agg.setMinTemperature(minTemp);
        agg.setMaxTemperature(maxTemp);
        agg.setAvgHumidity(avgHum);
        agg.setActiveSensors(uniqueNodes.size() > 0 ? uniqueNodes.size() : 1);
        agg.setEventCount(motionEvents);
        agg.setVectorClock(vc);
        agg.setReportingNodes(new ArrayList<>(uniqueNodes));

        // Send to Central Server
        communicationService.sendAggregatedDataToCentral(agg);

        history.add(agg);
        if (history.size() > 30) {
            history.remove(0);
        }

        log.info("Cluster {} Aggregation computed: AVG={:.1f}C, MIN={:.1f}C, MAX={:.1f}C, Hum={:.1f}%, Samples={}",
                nodeConfig.getClusterId(), avgTemp, minTemp, maxTemp, avgHum, count);

        return agg;
    }

    public List<AggregatedData> getHistory() {
        return new ArrayList<>(history);
    }
}
