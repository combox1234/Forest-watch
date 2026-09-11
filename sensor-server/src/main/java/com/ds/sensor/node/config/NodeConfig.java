package com.ds.sensor.node.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class NodeConfig {

    @Value("${node.id:S1}")
    private String nodeId;

    @Value("${node.cluster:C1}")
    private String clusterId;

    @Value("${server.port:8101}")
    private int port;

    @Value("${node.peers:}")
    private String peersString;

    @Value("${node.central-url:http://localhost:8000}")
    private String centralUrl;

    @Value("${node.is-initial-leader:false}")
    private boolean initialLeader;

    public NodeConfig() {}

    public NodeConfig(String nodeId, String clusterId, int port, String peersString, String centralUrl, boolean initialLeader) {
        this.nodeId = nodeId;
        this.clusterId = clusterId;
        this.port = port;
        this.peersString = peersString;
        this.centralUrl = centralUrl;
        this.initialLeader = initialLeader;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(1500))
                .setReadTimeout(Duration.ofMillis(1500))
                .build();
    }

    public String getNodeId() { return nodeId; }
    public String getClusterId() { return clusterId; }
    public int getPort() { return port; }
    public String getCentralUrl() { return centralUrl; }
    public boolean isInitialLeader() { return initialLeader; }

    public List<String> getPeerUrls() {
        List<String> list = new ArrayList<>();
        if (peersString != null && !peersString.trim().isEmpty()) {
            String[] parts = peersString.split(",");
            for (String p : parts) {
                if (!p.trim().isEmpty()) {
                    list.add(p.trim());
                }
            }
        }
        return list;
    }
}
