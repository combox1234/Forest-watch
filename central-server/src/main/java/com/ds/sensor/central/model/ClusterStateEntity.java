package com.ds.sensor.central.model;

import jakarta.persistence.*;

@Entity
@Table(name = "cluster_states")
public class ClusterStateEntity {

    @Id
    @Column(name = "cluster_id", length = 32)
    private String clusterId; // C1, C2

    @Column(name = "cluster_name", length = 64)
    private String clusterName; // Alpha Forest Canopy, Beta River Basin

    @Column(name = "current_head_id", length = 32)
    private String currentHeadId; // S2, S4 (or S1, S3 when promoted)

    @Column(name = "head_port")
    private int headPort;

    @Column(name = "active_members")
    private int activeMembers;

    @Column(name = "total_members")
    private int totalMembers;

    @Column(name = "last_updated")
    private long lastUpdated;

    public ClusterStateEntity() {
        this.lastUpdated = System.currentTimeMillis();
    }

    public ClusterStateEntity(String clusterId, String clusterName, String currentHeadId, int headPort, int totalMembers) {
        this.clusterId = clusterId;
        this.clusterName = clusterName;
        this.currentHeadId = currentHeadId;
        this.headPort = headPort;
        this.totalMembers = totalMembers;
        this.activeMembers = totalMembers;
        this.lastUpdated = System.currentTimeMillis();
    }

    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public String getClusterName() { return clusterName; }
    public void setClusterName(String clusterName) { this.clusterName = clusterName; }

    public String getCurrentHeadId() { return currentHeadId; }
    public void setCurrentHeadId(String currentHeadId) { this.currentHeadId = currentHeadId; }

    public int getHeadPort() { return headPort; }
    public void setHeadPort(int headPort) { this.headPort = headPort; }

    public int getActiveMembers() { return activeMembers; }
    public void setActiveMembers(int activeMembers) { this.activeMembers = activeMembers; }

    public int getTotalMembers() { return totalMembers; }
    public void setTotalMembers(int totalMembers) { this.totalMembers = totalMembers; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
}
