package com.ds.sensor.central.service;

import com.ds.sensor.central.model.ClusterStateEntity;
import com.ds.sensor.central.model.NodeEntity;
import com.ds.sensor.central.repository.*;
import com.ds.sensor.shared.message.VectorClockData;
import com.ds.sensor.shared.model.NodeRole;
import com.ds.sensor.shared.model.NodeStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class GlobalStateService {

    private final NodeRepository nodeRepository;
    private final ClusterStateRepository clusterStateRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final EventRepository eventRepository;
    private final AlertRepository alertRepository;
    private final AggregatedReadingRepository aggregatedReadingRepository;

    private final AtomicReference<VectorClockData> globalVectorClock = new AtomicReference<>(new VectorClockData());

    @Autowired
    public GlobalStateService(NodeRepository nodeRepository,
                              ClusterStateRepository clusterStateRepository,
                              SensorReadingRepository sensorReadingRepository,
                              EventRepository eventRepository,
                              AlertRepository alertRepository,
                              AggregatedReadingRepository aggregatedReadingRepository) {
        this.nodeRepository = nodeRepository;
        this.clusterStateRepository = clusterStateRepository;
        this.sensorReadingRepository = sensorReadingRepository;
        this.eventRepository = eventRepository;
        this.alertRepository = alertRepository;
        this.aggregatedReadingRepository = aggregatedReadingRepository;
    }

    public synchronized void updateGlobalClock(VectorClockData incoming) {
        if (incoming != null) {
            VectorClockData current = globalVectorClock.get();
            current.merge(incoming);
        }
    }

    public synchronized void resetGlobalClock() {
        globalVectorClock.set(new VectorClockData());
    }

    public VectorClockData getGlobalVectorClock() {
        return globalVectorClock.get();
    }

    public Map<String, Object> getSystemOverviewMetrics() {
        Map<String, Object> overview = new HashMap<>();

        List<NodeEntity> nodes = nodeRepository.findAll();
        long activeCount = nodes.stream().filter(n -> n.getStatus() == NodeStatus.ACTIVE).count();
        long activeChCount = nodes.stream().filter(n -> n.getStatus() == NodeStatus.ACTIVE && n.getRole() == NodeRole.CLUSTER_HEAD).count();

        overview.put("totalSensors", nodes.size() > 0 ? nodes.size() : 4);
        overview.put("activeSensors", activeCount);
        overview.put("totalClusters", 2);
        overview.put("activeClusterHeads", activeChCount);
        overview.put("totalRawReadings", sensorReadingRepository.count());
        overview.put("totalAggregatedBatches", aggregatedReadingRepository.count());
        overview.put("totalEvents", eventRepository.count());
        overview.put("totalAlerts", alertRepository.count());
        overview.put("globalVectorClock", globalVectorClock.get().getClock());
        overview.put("globalVectorClockString", globalVectorClock.get().toString());
        overview.put("globalVectorClockFormatted", globalVectorClock.get().toFormattedString());

        List<ClusterStateEntity> clusters = clusterStateRepository.findAll();
        overview.put("clusters", clusters);
        overview.put("nodes", nodes);

        return overview;
    }
}
