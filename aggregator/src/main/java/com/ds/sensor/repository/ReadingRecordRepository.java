package com.ds.sensor.repository;

import com.ds.sensor.model.ReadingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReadingRecordRepository extends JpaRepository<ReadingRecord, String> {
    List<ReadingRecord> findTop50ByOrderByTimestampDesc();
    List<ReadingRecord> findByClusterIdOrderByTimestampDesc(String clusterId);
}
