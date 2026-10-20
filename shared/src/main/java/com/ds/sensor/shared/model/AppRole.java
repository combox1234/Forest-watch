package com.ds.sensor.shared.model;

public enum AppRole {
    VIEWER,
    FOREST_OPERATOR,
    ADMINISTRATOR;

    public String getAuthority() {
        return "ROLE_" + name();
    }
}
