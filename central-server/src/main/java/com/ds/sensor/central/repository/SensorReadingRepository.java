package com.ds.sensor.central.repository;

import com.ds.sensor.central.model.SensorReadingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReadingEntity, Long> {
    List<SensorReadingEntity> findTop50ByOrderByTimestampDesc();
    List<SensorReadingEntity> findByNodeIdOrderByTimestampDesc(String nodeId);
}
