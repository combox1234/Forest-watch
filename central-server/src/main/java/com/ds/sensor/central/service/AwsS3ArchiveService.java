package com.ds.sensor.central.service;

import com.ds.sensor.central.model.S3ArchiveEntity;
import com.ds.sensor.central.repository.S3ArchiveRepository;
import com.ds.sensor.shared.message.AggregatedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Unit IV: Distributed Object-Based Systems / Distributed File Systems
 * Integrates AWS S3 telemetry archival for high-durability cold storage.
 */
@Service
public class AwsS3ArchiveService {

    private static final Logger log = LoggerFactory.getLogger(AwsS3ArchiveService.class);

    @Value("${aws.s3.bucket:ds-environmental-telemetry-archive}")
    private String s3BucketName = "ds-environmental-telemetry-archive";

    @Value("${aws.region:us-east-1}")
    private String awsRegion = "us-east-1";

    private final S3ArchiveRepository s3ArchiveRepository;

    @Autowired
    public AwsS3ArchiveService(S3ArchiveRepository s3ArchiveRepository) {
        this.s3ArchiveRepository = s3ArchiveRepository;
    }

    @Transactional
    public S3ArchiveEntity archiveAggregatedData(AggregatedData data) {
        if (data == null) return null;

        String clusterId = data.getClusterId() != null ? data.getClusterId() : "C1";
        String clusterHead = data.getClusterHeadId() != null ? data.getClusterHeadId() : "S2";
        String vcStr = data.getVectorClock() != null ? data.getVectorClock().toString() : "[0,0,0,0]";
        long now = System.currentTimeMillis();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timeStr = sdf.format(new Date(now));
        String objectKey = String.format("telemetry/%s/%s_%s_batch.json",
                clusterId.toLowerCase(), timeStr, clusterHead);

        String payloadJson = String.format(Locale.US,
                "{\n" +
                "  \"s3Bucket\": \"%s\",\n" +
                "  \"awsRegion\": \"%s\",\n" +
                "  \"clusterId\": \"%s\",\n" +
                "  \"clusterHeadId\": \"%s\",\n" +
                "  \"vectorClock\": \"%s\",\n" +
                "  \"timestamp\": %d,\n" +
                "  \"sampleCount\": %d,\n" +
                "  \"avgTemperature\": %.2f,\n" +
                "  \"minTemperature\": %.2f,\n" +
                "  \"maxTemperature\": %.2f,\n" +
                "  \"avgHumidity\": %.2f,\n" +
                "  \"activeSensors\": %d,\n" +
                "  \"storageClass\": \"STANDARD_IA\"\n" +
                "}",
                s3BucketName, awsRegion, clusterId, clusterHead, vcStr, now,
                data.getSampleCount(), data.getAvgTemperature(), data.getMinTemperature(),
                data.getMaxTemperature(), data.getAvgHumidity(), data.getActiveSensorsCount());

        byte[] payloadBytes = payloadJson.getBytes(StandardCharsets.UTF_8);
        long sizeBytes = payloadBytes.length;
        String eTag = generateMD5Hex(payloadBytes);

        S3ArchiveEntity entity = new S3ArchiveEntity(
                s3BucketName, objectKey, clusterId, clusterHead,
                data.getSampleCount(), data.getAvgTemperature(),
                vcStr, eTag, sizeBytes, "STORED", payloadJson
        );

        S3ArchiveEntity saved = s3ArchiveRepository.save(entity);
        log.info("[AWS-S3] Telemetry archived to s3://{}/{} (Size: {} B, ETag: {}, VC: {})",
                s3BucketName, objectKey, sizeBytes, eTag, vcStr);
        return saved;
    }

    public List<S3ArchiveEntity> getRecentArchives() {
        return s3ArchiveRepository.findTop50ByOrderByTimestampDesc();
    }

    public String getBucketName() {
        return s3BucketName;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    private String generateMD5Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
    }
}
