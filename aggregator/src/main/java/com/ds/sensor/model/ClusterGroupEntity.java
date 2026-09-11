package com.ds.sensor.model;

import jakarta.persistence.*;

@Entity
@Table(name = "cluster")
public class ClusterGroupEntity {

    @Id
    @Column(name = "cluster_id", length = 64)
    private String clusterId;

    @Column(name = "cluster_name", length = 128)
    private String clusterName;

    @Column(name = "leader_id", length = 64)
    private String leaderId;

    @Column(name = "status", length = 32)
    private String status; // ACTIVE, DEGRADED, ELECTION_IN_PROGRESS

    @Column(name = "last_election_time")
    private Long lastElectionTime;

    @Column(name = "member_count")
    private Integer memberCount = 0;

    public ClusterGroupEntity() {}

    public ClusterGroupEntity(String clusterId, String clusterName, String leaderId, String status) {
        this.clusterId = clusterId;
        this.clusterName = clusterName;
        this.leaderId = leaderId;
        this.status = status;
        this.lastElectionTime = System.currentTimeMillis();
    }

    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public String getClusterName() { return clusterName; }
    public void setClusterName(String clusterName) { this.clusterName = clusterName; }

    public String getLeaderId() { return leaderId; }
    public void setLeaderId(String leaderId) { this.leaderId = leaderId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getLastElectionTime() { return lastElectionTime; }
    public void setLastElectionTime(Long lastElectionTime) { this.lastElectionTime = lastElectionTime; }

    public Integer getMemberCount() { return memberCount; }
    public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }
}
