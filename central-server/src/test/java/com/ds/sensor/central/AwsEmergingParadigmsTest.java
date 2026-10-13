package com.ds.sensor.central;

import com.ds.sensor.central.model.LambdaInvocationEntity;
import com.ds.sensor.central.model.S3ArchiveEntity;
import com.ds.sensor.central.repository.LambdaInvocationRepository;
import com.ds.sensor.central.repository.S3ArchiveRepository;
import com.ds.sensor.central.service.AwsLambdaDispatcherService;
import com.ds.sensor.central.service.AwsS3ArchiveService;
import com.ds.sensor.shared.message.AggregatedData;
import com.ds.sensor.shared.message.AlertData;
import com.ds.sensor.shared.message.VectorClockData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit IV Test Suite:
 * Validates Distributed Object Storage (AWS S3) and Serverless Architecture (AWS Lambda).
 */
public class AwsEmergingParadigmsTest {

    private S3ArchiveRepository s3ArchiveRepository;
    private LambdaInvocationRepository lambdaInvocationRepository;
    private AwsS3ArchiveService s3ArchiveService;
    private AwsLambdaDispatcherService lambdaDispatcherService;

    @BeforeEach
    public void setup() {
        s3ArchiveRepository = Mockito.mock(S3ArchiveRepository.class);
        lambdaInvocationRepository = Mockito.mock(LambdaInvocationRepository.class);

        when(s3ArchiveRepository.save(any(S3ArchiveEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lambdaInvocationRepository.save(any(LambdaInvocationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        s3ArchiveService = new AwsS3ArchiveService(s3ArchiveRepository);
        lambdaDispatcherService = new AwsLambdaDispatcherService(lambdaInvocationRepository);
    }

    @Test
    public void testAwsS3DistributedObjectStorageArchival() {
        AggregatedData data = new AggregatedData();
        data.setClusterId("C1");
        data.setClusterHeadId("S2");
        data.setSampleCount(15);
        data.setAvgTemperature(25.4);
        data.setMinTemperature(21.2);
        data.setMaxTemperature(28.9);
        data.setAvgHumidity(60.1);
        data.setActiveSensors(2);
        data.setVectorClock(new VectorClockData(3, 4, 0, 0));

        S3ArchiveEntity entity = s3ArchiveService.archiveAggregatedData(data);

        assertNotNull(entity, "S3 Archive entity should not be null");
        assertTrue(entity.getObjectKey().startsWith("telemetry/c1/"), "Object key must follow hierarchical telemetry prefix");
        assertTrue(entity.getObjectKey().endsWith(".json"), "Object format must be JSON object");
        assertNotNull(entity.getETag(), "MD5 ETag must be computed for object integrity");
        assertTrue(entity.getSizeBytes() > 0, "Object byte size must be greater than 0");
        assertEquals("STORED", entity.getStatus());
        assertTrue(entity.getPayloadJson().contains("ds-environmental-telemetry-archive"));
    }

    @Test
    public void testAwsLambdaServerlessInvocation() {
        AlertData alert = new AlertData();
        alert.setAlertType("WILDFIRE_ALERT");
        alert.setClusterId("C2");
        alert.setSourceNodeId("S4");
        alert.setDescription("Critical ambient heat anomaly detected");
        alert.setVectorClock(new VectorClockData(0, 0, 1, 5));

        LambdaInvocationEntity invocation = lambdaDispatcherService.invokeAlertLambda(alert);

        assertNotNull(invocation, "Lambda invocation entity should not be null");
        assertNotNull(invocation.getRequestId(), "AWS Request ID must be populated");
        assertEquals(200, invocation.getStatusCode(), "Serverless function execution HTTP status should be 200 OK");
        assertEquals("EmergencyAlertDispatcher", invocation.getFunctionName());
        assertTrue(invocation.getDurationMs() >= 30, "Simulated execution duration must be recorded");
        assertTrue(invocation.getResponsePayload().contains("SNS_TOPIC_BROADCAST"));
    }

    @Test
    public void testLoadBalancerDispatchRoundRobin() {
        com.ds.sensor.central.controller.CentralApiController controller = new com.ds.sensor.central.controller.CentralApiController(
                null,
                null,
                null,
                s3ArchiveService,
                lambdaDispatcherService
        );

        org.springframework.http.ResponseEntity<java.util.Map<String, Object>> resp1 = controller.dispatchViaLoadBalancer();
        org.springframework.http.ResponseEntity<java.util.Map<String, Object>> resp2 = controller.dispatchViaLoadBalancer();

        assertEquals(200, resp1.getStatusCode().value());
        assertEquals(200, resp2.getStatusCode().value());
        assertNotEquals(resp1.getBody().get("handlingPod"), resp2.getBody().get("handlingPod"), "Round-robin must alternate between pods");

        org.springframework.http.ResponseEntity<java.util.Map<String, Object>> stats = controller.getLoadBalancerStats();
        assertEquals(2L, stats.getBody().get("totalRequestsRouted"));
    }
}
