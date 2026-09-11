package com.ds.sensor.repository;

import com.ds.sensor.model.ClusterGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClusterGroupRepository extends JpaRepository<ClusterGroupEntity, String> {
}
