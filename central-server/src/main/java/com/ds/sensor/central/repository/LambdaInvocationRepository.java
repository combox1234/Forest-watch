package com.ds.sensor.central.repository;

import com.ds.sensor.central.model.LambdaInvocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LambdaInvocationRepository extends JpaRepository<LambdaInvocationEntity, Long> {
    List<LambdaInvocationEntity> findTop50ByOrderByTimestampDesc();
}
