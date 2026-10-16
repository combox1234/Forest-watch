package com.ds.sensor.shared.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.Arrays;

public class VectorClockData implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int SIZE = 4; // S1, S2, S3, S4

    private int[] clock;

    public VectorClockData() {
        this.clock = new int[SIZE];
    }

    public VectorClockData(int[] clock) {
        if (clock != null && clock.length == SIZE) {
            this.clock = Arrays.copyOf(clock, SIZE);
        } else {
            this.clock = new int[SIZE];
        }
    }

    public VectorClockData(int s1, int s2, int s3, int s4) {
        this.clock = new int[]{s1, s2, s3, s4};
    }

    public int[] getClock() {
        return clock;
    }

    public void setClock(int[] clock) {
        this.clock = clock;
    }

    @JsonIgnore
    public static int nodeToIndex(String nodeId) {
        if (nodeId == null) return -1;
        String upper = nodeId.trim().toUpperCase();
        return switch (upper) {
            case "S1" -> 0;
            case "S2" -> 1;
            case "S3" -> 2;
            case "S4" -> 3;
            default -> -1;
        };
    }

    @JsonIgnore
    public static String indexToNode(int idx) {
        return switch (idx) {
            case 0 -> "S1";
            case 1 -> "S2";
            case 2 -> "S3";
            case 3 -> "S4";
            default -> "UNKNOWN";
        };
    }

    public synchronized void increment(int idx) {
        if (idx >= 0 && idx < SIZE) {
            this.clock[idx]++;
        }
    }

    public synchronized void increment(String nodeId) {
        int idx = nodeToIndex(nodeId);
        if (idx >= 0 && idx < SIZE) {
            this.clock[idx]++;
        }
    }

    public synchronized void merge(VectorClockData other) {
        if (other == null || other.clock == null) return;
        for (int i = 0; i < SIZE; i++) {
            this.clock[i] = Math.max(this.clock[i], other.clock[i]);
        }
    }

    public boolean happensBefore(VectorClockData other) {
        if (other == null || other.clock == null) return false;
        boolean atLeastOneLess = false;
        for (int i = 0; i < SIZE; i++) {
            if (this.clock[i] > other.clock[i]) {
                return false;
            }
            if (this.clock[i] < other.clock[i]) {
                atLeastOneLess = true;
            }
        }
        return atLeastOneLess;
    }

    public boolean isConcurrent(VectorClockData other) {
        if (other == null || other.clock == null) return false;
        return !this.happensBefore(other) && !other.happensBefore(this) && !this.equalsClock(other);
    }

    public boolean equalsClock(VectorClockData other) {
        if (other == null || other.clock == null) return false;
        return Arrays.equals(this.clock, other.clock);
    }

    public VectorClockData copy() {
        return new VectorClockData(this.clock);
    }

    public synchronized void reset() {
        if (this.clock != null) {
            Arrays.fill(this.clock, 0);
        } else {
            this.clock = new int[SIZE];
        }
    }

    public String toFormattedString() {
        if (clock == null || clock.length < SIZE) return "[S1:0, S2:0, S3:0, S4:0]";
        return String.format("[S1:%d, S2:%d, S3:%d, S4:%d]", clock[0], clock[1], clock[2], clock[3]);
    }

    @Override
    public String toString() {
        return Arrays.toString(clock);
    }
}
