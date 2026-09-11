package com.ds.sensor.repository;

import com.ds.sensor.model.HeartbeatRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HeartbeatRecordRepository extends JpaRepository<HeartbeatRecord, Long> {
    List<HeartbeatRecord> findTop50ByOrderByReceivedAtDesc();
    List<HeartbeatRecord> findBySensorIdOrderByReceivedAtDesc(String sensorId);
}
