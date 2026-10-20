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
        UserEntity admin = userRepository.findByUsername("priya.admin").orElse(null);
        org.junit.jupiter.api.Assertions.assertNotNull(admin, "Seed user 'priya.admin' must be present");
        assertTrue(passwordEncoder.matches("ForestWatch@2026", admin.getPassword()),
                "BCrypt password encoder must match the default demo password");
    }

    @Test
    @DisplayName("2. Login Form Success and Failure Handling")
    public void testLoginSuccessAndFailure() throws Exception {
        // Valid demo user credentials
        mockMvc.perform(formLogin().user("priya.admin").password("ForestWatch@2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // Invalid credentials
        mockMvc.perform(formLogin().user("priya.admin").password("WrongPassword999"))
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
    @WithMockUser(username = "priya.admin", roles = {"ADMINISTRATOR", "FOREST_OPERATOR", "VIEWER"})
    public void testAdminAllowedOnUserManagement() throws Exception {
        mockMvc.perform(get("/api/users").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

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
    }

    @Test
    @DisplayName("9. CSRF Protection Enforced on State-Changing Browser Requests")
    @WithMockUser(username = "priya.admin", roles = {"ADMINISTRATOR"})
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
}
