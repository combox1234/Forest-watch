package com.ds.sensor.central.model;

import com.ds.sensor.shared.model.AppRole;
import com.ds.sensor.shared.model.UserDto;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 128)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AppRole role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private long createdAt;

    public UserEntity() {
        this.createdAt = System.currentTimeMillis();
    }

    public UserEntity(String username, String password, String displayName, AppRole role, boolean enabled) {
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.role = role;
        this.enabled = enabled;
        this.createdAt = System.currentTimeMillis();
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public UserDto toDto() {
        return new UserDto(id, username, displayName, role, enabled, createdAt);
    }
}
