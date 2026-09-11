package com.ds.sensor.central.repository;

import com.ds.sensor.central.model.CommunicationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunicationLogRepository extends JpaRepository<CommunicationLogEntity, Long> {
    List<CommunicationLogEntity> findTop100ByOrderByTimestampDesc();
}
