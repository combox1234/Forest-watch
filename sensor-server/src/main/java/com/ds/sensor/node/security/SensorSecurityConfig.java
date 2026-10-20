package com.ds.sensor.node.security;

import com.ds.sensor.shared.model.SecurityConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SensorSecurityConfig {

    private final InternalServiceAuthFilter internalServiceAuthFilter;

    @Value("${forestwatch.security.demo-password:ForestWatch@2026}")
    private String defaultDemoPassword;

    @Value("${node.id:S1}")
    private String nodeId;

    @Autowired
    public SensorSecurityConfig(InternalServiceAuthFilter internalServiceAuthFilter) {
        this.internalServiceAuthFilter = internalServiceAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        String encodedPass = passwordEncoder.encode(defaultDemoPassword);

        UserDetails parthAdmin = User.withUsername("parth.admin")
                .password(encodedPass)
                .roles("ADMINISTRATOR", "FOREST_OPERATOR", "VIEWER")
                .build();

        UserDetails parthPatil = User.withUsername("parth.patil")
                .password(encodedPass)
                .roles("ADMINISTRATOR", "FOREST_OPERATOR", "VIEWER")
                .build();

        UserDetails arjun = User.withUsername("arjun.operator")
                .password(encodedPass)
                .roles("FOREST_OPERATOR", "VIEWER")
                .build();

        UserDetails ananya = User.withUsername("ananya.viewer")
                .password(encodedPass)
                .roles("VIEWER")
                .build();

        UserDetails rahul = User.withUsername("rahul.operator")
                .password(encodedPass)
                .roles("FOREST_OPERATOR", "VIEWER")
                .build();

        UserDetails kavya = User.withUsername("kavya.viewer")
                .password(encodedPass)
                .roles("VIEWER")
                .build();

        return new InMemoryUserDetailsManager(parthAdmin, parthPatil, arjun, ananya, rahul, kavya);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler requestHandler =
                new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        String cleanNodeId = nodeId != null ? nodeId.trim().toUpperCase() : "S1";
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepo.setCookieName("FW_" + cleanNodeId + "_XSRF");
        csrfRepo.setHeaderName("X-XSRF-TOKEN");

        http
            .addFilterBefore(internalServiceAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new CsrfCookieFilter(), org.springframework.security.web.authentication.www.BasicAuthenticationFilter.class)
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfRepo)
                .csrfTokenRequestHandler(requestHandler)
                .ignoringRequestMatchers(
                    "/api/message",
                    "/api/vector-clock/reset",
                    "/sensor/vector-clock/reset"
                )
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation().migrateSession()
            )
            .authorizeHttpRequests(auth -> auth
                // Public Assets and Pages
                .requestMatchers("/login", "/access-denied", "/error", "/css/**", "/js/**", "/images/**", "/favicon.ico", "/actuator/**").permitAll()

                // Inter-node and Central Server communication
                .requestMatchers(HttpMethod.POST,
                    "/api/message",
                    "/api/vector-clock/reset",
                    "/sensor/vector-clock/reset"
                ).hasAnyRole("SENSOR_SERVICE", "FOREST_OPERATOR", "ADMINISTRATOR")

                // Read-Only Monitoring & Telemetry APIs
                .requestMatchers(HttpMethod.GET, "/api/status", "/api/logs", "/api/auth/me").hasAnyRole("VIEWER", "FOREST_OPERATOR", "ADMINISTRATOR", "SENSOR_SERVICE")

                // Operational & Diagnostic Actions
                .requestMatchers(HttpMethod.POST,
                    "/api/generate-reading",
                    "/api/generate-event",
                    "/api/start-election",
                    "/api/simulate-failure",
                    "/api/recover",
                    "/api/aggregate-and-send"
                ).hasAnyRole("SENSOR_SERVICE", "FOREST_OPERATOR", "ADMINISTRATOR")

                // View Dashboard
                .requestMatchers("/").hasAnyRole("VIEWER", "FOREST_OPERATOR", "ADMINISTRATOR")

                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("FW_" + cleanNodeId + "_SESSION", "FW_" + cleanNodeId + "_XSRF", "JSESSIONID", "XSRF-TOKEN")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    if (isApiRequest(request)) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Authentication required. Please sign in.\"}");
                    } else {
                        response.sendRedirect("/login");
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    if (isApiRequest(request)) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write("{\"error\":\"FORBIDDEN\",\"message\":\"Access denied. Your role has insufficient privileges.\"}");
                    } else {
                        response.sendRedirect("/access-denied");
                    }
                })
            );

        return http.build();
    }

    @Bean
    public org.springframework.boot.web.server.WebServerFactoryCustomizer<org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory> sessionCookieCustomizer() {
        String cleanNodeId = nodeId != null ? nodeId.trim().toUpperCase() : "S1";
        return factory -> factory.addContextCustomizers(context -> context.setSessionCookieName("FW_" + cleanNodeId + "_SESSION"));
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        return uri.startsWith("/api/") || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE));
    }

    static final class CsrfCookieFilter extends org.springframework.web.filter.OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, jakarta.servlet.FilterChain filterChain)
                throws jakarta.servlet.ServletException, java.io.IOException {
            org.springframework.security.web.csrf.CsrfToken csrfToken =
                    (org.springframework.security.web.csrf.CsrfToken) request.getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
            if (csrfToken != null) {
                // Force evaluation of deferred token to write XSRF-TOKEN cookie
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}

