package com.ds.sensor.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GrpcServerManager {

    private static final Logger log = LoggerFactory.getLogger(GrpcServerManager.class);

    private final AggregationServiceImpl aggregationService;
    private Server server;

    @Value("${grpc.server.port:9090}")
    private int port;

    public GrpcServerManager(AggregationServiceImpl aggregationService) {
        this.aggregationService = aggregationService;
    }

    @PostConstruct
    public void start() throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(aggregationService)
                .build()
                .start();
        log.info("gRPC Server started, listening on port {}", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down gRPC server on JVM shutdown");
            stop();
        }));
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            log.info("Stopping gRPC server...");
            server.shutdown();
        }
    }

    public int getPort() {
        return port;
    }
}
