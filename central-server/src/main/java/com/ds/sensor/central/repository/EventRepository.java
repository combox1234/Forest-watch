package com.ds.sensor.central.repository;

import com.ds.sensor.central.model.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {
    List<EventEntity> findTop50ByOrderByTimestampDesc();
    List<EventEntity> findByClusterIdOrderByTimestampDesc(String clusterId);
}
