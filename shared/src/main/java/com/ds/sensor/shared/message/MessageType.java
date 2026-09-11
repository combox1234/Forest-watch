package com.ds.sensor.shared.message;

public enum MessageType {
    HEARTBEAT,
    HEARTBEAT_ACK,
    SENSOR_DATA,
    EVENT,
    ELECTION,
    ELECTION_OK,
    ANSWER,
    COORDINATOR,
    ALERT_REQUEST,
    ALERT_GRANTED,
    ALERT_REJECTED,
    AGGREGATED_DATA,
    NODE_STATUS,
    RECOVERY
}
