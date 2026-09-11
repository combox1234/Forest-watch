package com.ds.sensor.repository;

import com.ds.sensor.model.ElectionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ElectionRecordRepository extends JpaRepository<ElectionRecord, String> {
    List<ElectionRecord> findTop50ByOrderByStartedAtDesc();
    List<ElectionRecord> findByClusterIdOrderByStartedAtDesc(String clusterId);
}
