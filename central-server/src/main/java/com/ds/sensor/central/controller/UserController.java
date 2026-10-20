package com.ds.sensor.central.controller;

import com.ds.sensor.central.model.AlertEntity;
import com.ds.sensor.central.model.UserEntity;
import com.ds.sensor.central.repository.UserRepository;
import com.ds.sensor.central.service.CentralService;
import com.ds.sensor.shared.model.AppRole;
import com.ds.sensor.shared.model.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserRepository userRepository;
    private final CentralService centralService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserController(UserRepository userRepository,
                          CentralService centralService,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.centralService = centralService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/auth/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = authentication.getName();
        Optional<UserEntity> userOpt = userRepository.findByUsername(username);

        String displayName = userOpt.map(UserEntity::getDisplayName).orElse(username);
        String role = userOpt.map(u -> u.getRole().name()).orElse("VIEWER");

        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("username", username);
        response.put("displayName", displayName);
        response.put("role", role);
        response.put("authorities", authorities);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<List<UserDto>> listUsers() {
        List<UserDto> dtos = userRepository.findAll().stream()
                .map(UserEntity::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String displayName = request.get("displayName");
        String password = request.get("password");
        String roleStr = request.get("role");

        if (username == null || username.trim().isEmpty() ||
            displayName == null || displayName.trim().isEmpty() ||
            password == null || password.trim().isEmpty() ||
            roleStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "All fields (username, displayName, password, role) are required"));
        }

        username = username.trim().toLowerCase();
        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Username already exists"));
        }

        AppRole role;
        try {
            role = AppRole.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role: " + roleStr));
        }

        UserEntity newUser = new UserEntity(
                username,
                passwordEncoder.encode(password),
                displayName.trim(),
                role,
                true
        );

        UserEntity saved = userRepository.save(newUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toDto());
    }

    @PutMapping("/users/{username}/role")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> updateUserRole(@PathVariable String username,
                                            @RequestParam(required = false) String role,
                                            @RequestBody(required = false) Map<String, String> request,
                                            Authentication auth) {
        String roleStr = (role != null) ? role : (request != null ? request.get("role") : null);
        if (roleStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Role is required"));
        }

        AppRole newRole;
        try {
            newRole = AppRole.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role: " + roleStr));
        }

        Optional<UserEntity> userOpt = userRepository.findByUsername(username.toLowerCase());
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        UserEntity user = userOpt.get();
        // Prevent accidental self-demotion from Administrator
        if (auth.getName().equalsIgnoreCase(username) && newRole != AppRole.ADMINISTRATOR) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot demote your own administrator account"));
        }

        user.setRole(newRole);
        userRepository.save(user);

        return ResponseEntity.ok(user.toDto());
    }

    @PutMapping("/users/{username}/status")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<?> updateUserStatus(@PathVariable String username,
                                              @RequestParam(required = false) Boolean enabled,
                                              @RequestBody(required = false) Map<String, Boolean> request,
                                              Authentication auth) {
        Boolean isEnabled = (enabled != null) ? enabled : (request != null ? request.get("enabled") : null);
        if (isEnabled == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "'enabled' boolean field is required"));
        }

        Optional<UserEntity> userOpt = userRepository.findByUsername(username.toLowerCase());
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Prevent self-disabling
        if (auth.getName().equalsIgnoreCase(username) && !isEnabled) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot disable your own active account"));
        }

        UserEntity user = userOpt.get();
        user.setEnabled(isEnabled);
        userRepository.save(user);

        return ResponseEntity.ok(user.toDto());
    }

    @PostMapping("/alerts/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('FOREST_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<?> acknowledgeAlert(@PathVariable Long id, Authentication auth) {
        String operatorName = auth != null ? auth.getName() : "OPERATOR";
        AlertEntity acknowledged = centralService.acknowledgeAlert(id, operatorName);
        if (acknowledged == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
                "status", "ACKNOWLEDGED",
                "alertId", acknowledged.getId(),
                "acknowledgedBy", operatorName,
                "timestamp", System.currentTimeMillis()
        ));
    }
}
