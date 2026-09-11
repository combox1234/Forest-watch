package com.ds.sensor.model;

import jakarta.persistence.*;

@Entity
@Table(name = "election_log")
public class ElectionRecord {

    @Id
    @Column(name = "election_id", length = 64)
    private String electionId;

    @Column(name = "cluster_id", length = 64, nullable = false)
    private String clusterId;

    @Column(name = "initiator", length = 64)
    private String initiator;

    @Column(name = "elected_leader", length = 64)
    private String electedLeader;

    @Column(name = "started_at")
    private Long startedAt;

    @Column(name = "completed_at")
    private Long completedAt;

    @Column(name = "reason", length = 256)
    private String reason; // LEADER_TIMEOUT, HIGHER_PRIORITY_ONLINE, MANUAL_TRIGGER

    @Column(name = "duration_ms")
    private Long durationMs;

    public ElectionRecord() {}

    public String getElectionId() { return electionId; }
    public void setElectionId(String electionId) { this.electionId = electionId; }

    public String getClusterId() { return clusterId; }
    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public String getInitiator() { return initiator; }
    public void setInitiator(String initiator) { this.initiator = initiator; }

    public String getElectedLeader() { return electedLeader; }
    public void setElectedLeader(String electedLeader) { this.electedLeader = electedLeader; }

    public Long getStartedAt() { return startedAt; }
    public void setStartedAt(Long startedAt) { this.startedAt = startedAt; }

    public Long getCompletedAt() { return completedAt; }
    public void setCompletedAt(Long completedAt) { this.completedAt = completedAt; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
}
