package com.ds.sensor.central.service;

import com.ds.sensor.central.model.LambdaInvocationEntity;
import com.ds.sensor.central.repository.LambdaInvocationRepository;
import com.ds.sensor.shared.message.AlertData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Unit IV: Serverless Architectures & Case Studies: AWS
 * Dispatches emergency alert events to an AWS Lambda serverless handler.
 */
@Service
public class AwsLambdaDispatcherService {

    private static final Logger log = LoggerFactory.getLogger(AwsLambdaDispatcherService.class);

    @Value("${aws.lambda.function-name:EmergencyAlertDispatcher}")
    private String lambdaFunctionName = "EmergencyAlertDispatcher";

    @Value("${aws.region:us-east-1}")
    private String awsRegion = "us-east-1";

    private final LambdaInvocationRepository lambdaInvocationRepository;
    private final Random random = new Random();

    @Autowired
    public AwsLambdaDispatcherService(LambdaInvocationRepository lambdaInvocationRepository) {
        this.lambdaInvocationRepository = lambdaInvocationRepository;
    }

    @Transactional
    public LambdaInvocationEntity invokeAlertLambda(AlertData alertData) {
        if (alertData == null) return null;

        String alertType = alertData.getAlertType() != null ? alertData.getAlertType() : "EMERGENCY_ALERT";
        String clusterId = alertData.getClusterId() != null ? alertData.getClusterId() : "C1";
        String sourceNode = alertData.getSourceNodeId() != null ? alertData.getSourceNodeId() : "S1";
        String vcStr = alertData.getVectorClock() != null ? alertData.getVectorClock().toString() : "[0,0,0,0]";

        return executeInvocation("MUTEX_ALERT: " + alertType, clusterId, sourceNode, vcStr);
    }

    @Transactional
    public LambdaInvocationEntity triggerManualTest(String alertType, String clusterId, String sourceNode, String vectorClock) {
        return executeInvocation("MANUAL_TRIGGER: " + alertType, clusterId, sourceNode, vectorClock);
    }

    private LambdaInvocationEntity executeInvocation(String triggerEvent, String clusterId, String sourceNode, String vectorClock) {
        String requestId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        // Serverless execution: simulate sub-100ms invocation latency
        long durationMs = 35 + random.nextInt(45); // 35ms - 80ms

        String responseJson = String.format(Locale.US,
                "{\n" +
                "  \"function\": \"%s\",\n" +
                "  \"region\": \"%s\",\n" +
                "  \"awsRequestId\": \"%s\",\n" +
                "  \"statusCode\": 200,\n" +
                "  \"action\": \"SNS_TOPIC_BROADCAST\",\n" +
                "  \"targetTopicArn\": \"arn:aws:sns:%s:123456789012:EnvironmentalAlerts\",\n" +
                "  \"event\": \"%s\",\n" +
                "  \"clusterId\": \"%s\",\n" +
                "  \"sourceNode\": \"%s\",\n" +
                "  \"vectorClock\": \"%s\",\n" +
                "  \"memoryUsedMB\": 48,\n" +
                "  \"billedDurationMs\": %d\n" +
                "}",
                lambdaFunctionName, awsRegion, requestId, awsRegion,
                triggerEvent, clusterId, sourceNode, vectorClock, durationMs);

        LambdaInvocationEntity entity = new LambdaInvocationEntity(
                requestId,
                lambdaFunctionName,
                triggerEvent,
                clusterId,
                sourceNode,
                durationMs,
                200,
                vectorClock,
                responseJson
        );

        LambdaInvocationEntity saved = lambdaInvocationRepository.save(entity);
        log.info("[AWS-Lambda] Serverless Invocation SUCCESS: function='{}', requestId='{}', duration={}ms, trigger='{}'",
                lambdaFunctionName, requestId, durationMs, triggerEvent);
        return saved;
    }

    public List<LambdaInvocationEntity> getRecentInvocations() {
        return lambdaInvocationRepository.findTop50ByOrderByTimestampDesc();
    }

    public String getLambdaFunctionName() {
        return lambdaFunctionName;
    }

    public String getAwsRegion() {
        return awsRegion;
    }
}
