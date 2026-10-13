package com.ds.sensor.central.repository;

import com.ds.sensor.central.model.S3ArchiveEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface S3ArchiveRepository extends JpaRepository<S3ArchiveEntity, Long> {
    List<S3ArchiveEntity> findTop50ByOrderByTimestampDesc();
}
