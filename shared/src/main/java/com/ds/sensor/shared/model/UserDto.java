package com.ds.sensor.shared.model;

import java.io.Serializable;

public class UserDto implements Serializable {

    private Long id;
    private String username;
    private String displayName;
    private AppRole role;
    private boolean enabled;
    private long createdAt;

    public UserDto() {}

    public UserDto(Long id, String username, String displayName, AppRole role, boolean enabled, long createdAt) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.role = role;
        this.enabled = enabled;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public AppRole getRole() {
        return role;
    }

    public void setRole(AppRole role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
