package com.ds.sensor.central.repository;

import com.ds.sensor.central.model.NodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NodeRepository extends JpaRepository<NodeEntity, String> {
    List<NodeEntity> findByClusterId(String clusterId);
}
