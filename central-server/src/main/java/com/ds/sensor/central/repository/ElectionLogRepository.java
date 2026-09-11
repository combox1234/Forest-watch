package com.ds.sensor.central.repository;

import com.ds.sensor.central.model.ElectionLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ElectionLogRepository extends JpaRepository<ElectionLogEntity, Long> {
    List<ElectionLogEntity> findTop50ByOrderByTimestampDesc();
}
