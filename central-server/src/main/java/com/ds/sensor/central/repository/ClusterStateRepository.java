package com.ds.sensor.central.repository;

import com.ds.sensor.central.model.ClusterStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClusterStateRepository extends JpaRepository<ClusterStateEntity, String> {
}
