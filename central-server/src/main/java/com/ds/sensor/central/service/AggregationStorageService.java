package com.ds.sensor.central.service;

import com.ds.sensor.central.model.AggregatedReadingEntity;
import com.ds.sensor.central.repository.AggregatedReadingRepository;
import com.ds.sensor.shared.message.AggregatedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AggregationStorageService {

    private static final Logger log = LoggerFactory.getLogger(AggregationStorageService.class);

    private final AggregatedReadingRepository aggregatedReadingRepository;
    private final GlobalStateService globalStateService;
    private final AwsS3ArchiveService awsS3ArchiveService;

    @Autowired
    public AggregationStorageService(AggregatedReadingRepository aggregatedReadingRepository,
                                     GlobalStateService globalStateService,
                                     AwsS3ArchiveService awsS3ArchiveService) {
        this.aggregatedReadingRepository = aggregatedReadingRepository;
        this.globalStateService = globalStateService;
        this.awsS3ArchiveService = awsS3ArchiveService;
    }

    @Transactional
    public AggregatedReadingEntity saveAggregatedData(AggregatedData data) {
        if (data == null) return null;

        // Archive batch to AWS S3 Distributed Object Storage (Unit IV)
        try {
            awsS3ArchiveService.archiveAggregatedData(data);
        } catch (Exception e) {
            log.warn("Non-blocking AWS S3 archival notice: {}", e.getMessage());
        }

        String vcStr = data.getVectorClock() != null ? data.getVectorClock().toString() : "[]";
        AggregatedReadingEntity entity = new AggregatedReadingEntity(
                data.getClusterId(),
                data.getClusterHeadId(),
                data.getSampleCount(),
                data.getAvgTemperature(),
                data.getMinTemperature(),
                data.getMaxTemperature(),
                data.getAvgHumidity(),
                data.getActiveSensorsCount(),
                data.getMotionEventsCount(),
                vcStr
        );

        if (data.getVectorClock() != null) {
            globalStateService.updateGlobalClock(data.getVectorClock());
        }

        AggregatedReadingEntity saved = aggregatedReadingRepository.save(entity);
        log.info("Saved edge aggregate from cluster {} (CH={}): AVG temp={:.1f}, MIN={:.1f}, MAX={:.1f}, samples={}",
                data.getClusterId(), data.getClusterHeadId(), data.getAvgTemperature(),
                data.getMinTemperature(), data.getMaxTemperature(), data.getSampleCount());
        return saved;
    }

    public List<AggregatedReadingEntity> getRecentAggregatedReadings() {
        return aggregatedReadingRepository.findTop50ByOrderByTimestampDesc();
    }
}
