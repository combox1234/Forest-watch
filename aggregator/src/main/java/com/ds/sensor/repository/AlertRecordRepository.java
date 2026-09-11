package com.ds.sensor.repository;

import com.ds.sensor.model.AlertRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlertRecordRepository extends JpaRepository<AlertRecord, String> {
    List<AlertRecord> findTop50ByOrderByEventTimeDesc();
    List<AlertRecord> findByClusterIdOrderByEventTimeDesc(String clusterId);
}
