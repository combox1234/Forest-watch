package com.ds.sensor.central.repository;

import com.ds.sensor.central.model.AlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<AlertEntity, Long> {
    List<AlertEntity> findTop50ByOrderByTimestampDesc();
    List<AlertEntity> findByClusterIdOrderByTimestampDesc(String clusterId);
}
