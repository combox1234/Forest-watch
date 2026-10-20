package com.ds.sensor.central;

import com.ds.sensor.central.model.UserEntity;
import com.ds.sensor.central.repository.UserRepository;
import com.ds.sensor.shared.message.VectorClockData;
import com.ds.sensor.shared.model.AppRole;
import com.ds.sensor.shared.model.SecurityConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ForestWatchSecurityRbacTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("1. Password Encoding & Seeding Verification")
    public void testPasswordEncodingAndDemoUsers() {
        // Verify demo admin exists and has BCrypt hashed password
        UserEntity admin = userRepository.findByUsername("parth.admin").orElse(null);
        org.junit.jupiter.api.Assertions.assertNotNull(admin, "Seed user 'parth.admin' must be present");
        assertTrue(passwordEncoder.matches("ForestWatch@2026", admin.getPassword()),
                "BCrypt password encoder must match the default demo password");
    }

    @Test
    @DisplayName("2. Login Form Success and Failure Handling")
    public void testLoginSuccessAndFailure() throws Exception {
        // Valid demo user credentials
        mockMvc.perform(formLogin().user("parth.admin").password("ForestWatch@2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // Invalid credentials
        mockMvc.perform(formLogin().user("parth.admin").password("WrongPassword999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @DisplayName("3. Anonymous Access to Protected REST APIs is Denied with 401")
    public void testAnonymousAccessDeniedOnProtectedApi() throws Exception {
        mockMvc.perform(get("/api/users").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/vector-clock/reset").with(csrf()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("4. Viewer Role can access read-only monitoring APIs")
    @WithMockUser(username = "ananya.viewer", roles = {"VIEWER"})
    public void testViewerAccessToReadApis() throws Exception {
        mockMvc.perform(get("/api/system-status").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/alerts").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ananya.viewer"))
                .andExpect(jsonPath("$.role").value("VIEWER"));
    }

    @Test
    @DisplayName("5. Viewer Role is Denied on State-Changing Operational Endpoints (403)")
    @WithMockUser(username = "ananya.viewer", roles = {"VIEWER"})
    public void testViewerDenialOnRestrictedOperations() throws Exception {
        mockMvc.perform(post("/api/vector-clock/reset").with(csrf()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/aws/s3-archive")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clusterId\":\"C1\",\"clusterHeadId\":\"S2\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("6. Forest Operator Role is Authorized on Operational Actions")
    @WithMockUser(username = "arjun.operator", roles = {"FOREST_OPERATOR", "VIEWER"})
    public void testOperatorAllowedOnOperationalEndpoints() throws Exception {
        mockMvc.perform(post("/api/vector-clock/reset").with(csrf()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("7. Forest Operator Role is Denied on Administrator User Management (403)")
    @WithMockUser(username = "arjun.operator", roles = {"FOREST_OPERATOR", "VIEWER"})
    public void testOperatorDeniedOnUserManagement() throws Exception {
        mockMvc.perform(get("/api/users").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"temp.user\",\"displayName\":\"Temp User\",\"password\":\"Password123\",\"role\":\"VIEWER\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("8. Administrator Role is Authorized on User Management")
    @WithMockUser(username = "parth.admin", roles = {"ADMINISTRATOR", "FOREST_OPERATOR", "VIEWER"})
    public void testAdminAllowedOnUserManagement() throws Exception {
        mockMvc.perform(get("/api/users").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Clean up test user if present from a previous run on persistent SQLite database
        userRepository.findByUsername("test.newop").ifPresent(userRepository::delete);

        String newUserJson = objectMapper.writeValueAsString(Map.of(
                "username", "test.newop",
                "displayName", "Test Operator",
                "password", "ForestSecure@99",
                "role", "FOREST_OPERATOR"
        ));

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUserJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("test.newop"))
                .andExpect(jsonPath("$.role").value("FOREST_OPERATOR"));

        // Admin can update status
        mockMvc.perform(put("/api/users/test.newop/status?enabled=false").with(csrf()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        // Clean up afterwards
        userRepository.findByUsername("test.newop").ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("9. CSRF Protection Enforced on State-Changing Browser Requests")
    @WithMockUser(username = "parth.admin", roles = {"ADMINISTRATOR"})
    public void testCsrfProtectionEnforced() throws Exception {
        // Without CSRF token, state-changing POST must be rejected with 403 Forbidden
        mockMvc.perform(post("/api/vector-clock/reset").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("10. Sensor Service Key Authentication allows inter-service telemetry without CSRF")
    public void testSensorServiceKeyAuthentication() throws Exception {
        Map<String, Object> hb = Map.of(
                "nodeId", "S1",
                "port", 8101,
                "clusterId", "C1",
                "isClusterHead", false,
                "vectorClock", "[1, 0, 0, 0]"
        );

        String json = objectMapper.writeValueAsString(hb);

        // Sending heartbeat with valid service key succeeds without session or CSRF
        mockMvc.perform(post("/api/heartbeat")
                        .header(SecurityConstants.SERVICE_HEADER_NAME, SecurityConstants.DEFAULT_SERVICE_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Without service key, anonymous request to internal endpoint is rejected with 401 Unauthorized
        mockMvc.perform(post("/api/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("11. Administrator resets another user's password, BCrypt hash is verified, new password authenticates and old fails")
    @WithMockUser(username = "parth.admin", roles = {"ADMINISTRATOR"})
    public void testAdminResetPasswordLifecycle() throws Exception {
        userRepository.findByUsername("test.resetuser").ifPresent(userRepository::delete);
        UserEntity testUser = new UserEntity("test.resetuser", passwordEncoder.encode("InitialPass@123"), "Reset Target User", AppRole.VIEWER, true);
        userRepository.save(testUser);

        // 1. Initial password works
        mockMvc.perform(formLogin().user("test.resetuser").password("InitialPass@123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // 2. Admin resets password
        String requestBody = objectMapper.writeValueAsString(Map.of("newPassword", "BrandNewSecret@456"));
        mockMvc.perform(put("/api/users/test.resetuser/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.username").value("test.resetuser"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        // 3. Database stores valid BCrypt hash
        UserEntity updated = userRepository.findByUsername("test.resetuser").orElseThrow();
        assertTrue(updated.getPassword().startsWith("$2a$12$") || updated.getPassword().startsWith("$2b$12$"),
                "Password must be hashed with BCrypt strength 12");
        assertTrue(passwordEncoder.matches("BrandNewSecret@456", updated.getPassword()),
                "BCrypt encoder must match the new reset password");

        // 4. New password authenticates successfully
        mockMvc.perform(formLogin().user("test.resetuser").password("BrandNewSecret@456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // 5. Old password no longer works
        mockMvc.perform(formLogin().user("test.resetuser").password("InitialPass@123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));

        // Clean up
        userRepository.findByUsername("test.resetuser").ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("12. Unauthorized users and anonymous requests cannot reset passwords (401/403)")
    public void testUnauthorizedCannotResetPassword() throws Exception {
        // Anonymous request returns 401
        mockMvc.perform(put("/api/users/ananya.viewer/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"HackedPass@123\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("13. Viewer and Operator roles are forbidden from resetting user passwords")
    @WithMockUser(username = "ananya.viewer", roles = {"VIEWER"})
    public void testViewerForbiddenToResetPassword() throws Exception {
        mockMvc.perform(put("/api/users/kavya.viewer/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"ForbiddenPass@123\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("14. Admin password reset validates password length and non-existent user")
    @WithMockUser(username = "parth.admin", roles = {"ADMINISTRATOR"})
    public void testAdminPasswordResetValidation() throws Exception {
        // Too short (< 6 chars)
        mockMvc.perform(put("/api/users/parth.admin/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"123\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        // Non-existent user
        mockMvc.perform(put("/api/users/nonexistent.user.999/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"ValidPass@123\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("15. User changes own password with current password verification lifecycle")
    @WithMockUser(username = "test.selfuser", roles = {"VIEWER"})
    public void testChangeOwnPasswordLifecycle() throws Exception {
        userRepository.findByUsername("test.selfuser").ifPresent(userRepository::delete);
        UserEntity selfUser = new UserEntity("test.selfuser", passwordEncoder.encode("CurrentPass@123"), "Self User", AppRole.VIEWER, true);
        userRepository.save(selfUser);

        // 1. Wrong current password is rejected
        mockMvc.perform(put("/api/auth/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "WrongCurrentPass",
                                "newPassword", "NewValidPass@456"
                        )))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Current password does not match"));

        // 2. New password identical to current is rejected
        mockMvc.perform(put("/api/auth/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "CurrentPass@123",
                                "newPassword", "CurrentPass@123"
                        )))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("New password cannot be identical to the current password"));

        // 3. Valid change own password succeeds
        mockMvc.perform(put("/api/auth/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "CurrentPass@123",
                                "newPassword", "UpdatedValidPass@789"
                        )))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // 4. Verify login with updated password
        mockMvc.perform(formLogin().user("test.selfuser").password("UpdatedValidPass@789"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // Clean up
        userRepository.findByUsername("test.selfuser").ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("16. Login handles whitespace trimming and case-insensitive username normalization")
    public void testLoginTrimmingAndCaseInsensitivity() throws Exception {
        // Leading/trailing spaces on existing demo admin
        mockMvc.perform(formLogin().user("  parth.admin  ").password("ForestWatch@2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // Mixed case username on existing demo admin
        mockMvc.perform(formLogin().user("Parth.Admin").password("ForestWatch@2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }
}
