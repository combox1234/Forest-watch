package com.ds.sensor.controller;

import com.ds.sensor.model.*;
import com.ds.sensor.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {

    private final SensorNodeRepository sensorRepository;
    private final ClusterGroupRepository clusterRepository;
    private final ReadingRecordRepository readingRepository;
    private final AlertRecordRepository alertRepository;
    private final ElectionRecordRepository electionRepository;
    private final HeartbeatRecordRepository heartbeatRepository;

    public ApiController(
            SensorNodeRepository sensorRepository,
            ClusterGroupRepository clusterRepository,
            ReadingRecordRepository readingRepository,
            AlertRecordRepository alertRepository,
            ElectionRecordRepository electionRepository,
            HeartbeatRecordRepository heartbeatRepository) {
        this.sensorRepository = sensorRepository;
        this.clusterRepository = clusterRepository;
        this.readingRepository = readingRepository;
        this.alertRepository = alertRepository;
        this.electionRepository = electionRepository;
        this.heartbeatRepository = heartbeatRepository;
    }

    @GetMapping("/sensors")
    public ResponseEntity<List<SensorNodeEntity>> getSensors() {
        return ResponseEntity.ok(sensorRepository.findAll());
    }

    @GetMapping("/clusters")
    public ResponseEntity<List<ClusterGroupEntity>> getClusters() {
        return ResponseEntity.ok(clusterRepository.findAll());
    }

    @GetMapping("/readings")
    public ResponseEntity<List<ReadingRecord>> getReadings(
            @RequestParam(required = false) String clusterId) {
        if (clusterId != null && !clusterId.isEmpty()) {
            return ResponseEntity.ok(readingRepository.findByClusterIdOrderByTimestampDesc(clusterId));
        }
        return ResponseEntity.ok(readingRepository.findTop50ByOrderByTimestampDesc());
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<AlertRecord>> getAlerts(
            @RequestParam(required = false) String clusterId) {
        if (clusterId != null && !clusterId.isEmpty()) {
            return ResponseEntity.ok(alertRepository.findByClusterIdOrderByEventTimeDesc(clusterId));
        }
        return ResponseEntity.ok(alertRepository.findTop50ByOrderByEventTimeDesc());
    }

    @GetMapping("/elections")
    public ResponseEntity<List<ElectionRecord>> getElections(
            @RequestParam(required = false) String clusterId) {
        if (clusterId != null && !clusterId.isEmpty()) {
            return ResponseEntity.ok(electionRepository.findByClusterIdOrderByStartedAtDesc(clusterId));
        }
        return ResponseEntity.ok(electionRepository.findTop50ByOrderByStartedAtDesc());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        List<SensorNodeEntity> sensors = sensorRepository.findAll();
        long healthyCount = sensors.stream().filter(s -> "HEALTHY".equalsIgnoreCase(s.getStatus())).count();
        long failedCount = sensors.stream().filter(s -> "FAILED".equalsIgnoreCase(s.getStatus())).count();
        long suspectedCount = sensors.stream().filter(s -> "SUSPECTED".equalsIgnoreCase(s.getStatus())).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalNodes", sensors.size());
        stats.put("healthyNodes", healthyCount);
        stats.put("failedNodes", failedCount);
        stats.put("suspectedNodes", suspectedCount);
        stats.put("totalClusters", clusterRepository.count());
        stats.put("totalAlerts", alertRepository.count());
        stats.put("totalSummaries", readingRepository.count());
        stats.put("totalElections", electionRepository.count());
        return ResponseEntity.ok(stats);
    }
}
