package com.ds.sensor.central.repository;

import com.ds.sensor.central.model.AggregatedReadingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AggregatedReadingRepository extends JpaRepository<AggregatedReadingEntity, Long> {
    List<AggregatedReadingEntity> findTop50ByOrderByTimestampDesc();
    List<AggregatedReadingEntity> findByClusterIdOrderByTimestampDesc(String clusterId);
}
