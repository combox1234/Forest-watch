package com.ds.sensor.shared.model;

public final class SecurityConstants {

    private SecurityConstants() {}

    public static final String SERVICE_HEADER_NAME = "X-ForestWatch-Service-Key";
    public static final String DEFAULT_SERVICE_KEY = "forestwatch-internal-sensor-key-2026";
    public static final String ROLE_SENSOR_SERVICE = "ROLE_SENSOR_SERVICE";
    public static final String ROLE_VIEWER = "ROLE_VIEWER";
    public static final String ROLE_FOREST_OPERATOR = "ROLE_FOREST_OPERATOR";
    public static final String ROLE_ADMINISTRATOR = "ROLE_ADMINISTRATOR";
}
