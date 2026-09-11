package com.ds.sensor.repository;

import com.ds.sensor.model.SensorNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensorNodeRepository extends JpaRepository<SensorNodeEntity, String> {
    List<SensorNodeEntity> findByClusterId(String clusterId);
    List<SensorNodeEntity> findByStatus(String status);
}
