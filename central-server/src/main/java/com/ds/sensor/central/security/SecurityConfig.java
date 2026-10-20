package com.ds.sensor.central.security;

import com.ds.sensor.central.service.ForestWatchUserDetailsService;
import com.ds.sensor.shared.model.SecurityConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ForestWatchUserDetailsService userDetailsService;
    private final InternalServiceAuthFilter internalServiceAuthFilter;

    @Autowired
    public SecurityConfig(ForestWatchUserDetailsService userDetailsService,
                          InternalServiceAuthFilter internalServiceAuthFilter) {
        this.userDetailsService = userDetailsService;
        this.internalServiceAuthFilter = internalServiceAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler requestHandler =
                new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepo.setCookieName("FW_CENTRAL_XSRF");
        csrfRepo.setHeaderName("X-XSRF-TOKEN");

        http
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(internalServiceAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new CsrfCookieFilter(), org.springframework.security.web.authentication.www.BasicAuthenticationFilter.class)
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfRepo)
                .csrfTokenRequestHandler(requestHandler)
                // Internal service-to-service endpoints authenticated via service key don't require browser CSRF
                .ignoringRequestMatchers(
                    "/api/heartbeat",
                    "/api/nodes/register",
                    "/api/node-status",
                    "/api/cluster-head",
                    "/api/election-event",
                    "/api/aggregate",
                    "/api/reading",
                    "/api/event",
                    "/api/alert",
                    "/api/log-message"
                )
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation().migrateSession()
            )
            .authorizeHttpRequests(auth -> auth
                // Public Assets and Pages
                .requestMatchers("/login", "/access-denied", "/error", "/css/**", "/js/**", "/images/**", "/favicon.ico", "/h2-console/**", "/actuator/**").permitAll()

                // Internal Sensor Service Communication Endpoints
                .requestMatchers(HttpMethod.POST,
                    "/api/heartbeat",
                    "/api/nodes/register",
                    "/api/node-status",
                    "/api/cluster-head",
                    "/api/election-event",
                    "/api/aggregate",
                    "/api/reading",
                    "/api/event",
                    "/api/alert",
                    "/api/log-message"
                ).hasAnyRole("SENSOR_SERVICE", "FOREST_OPERATOR", "ADMINISTRATOR")

                // Administrator-Only Endpoints
                .requestMatchers("/users/**", "/api/users/**").hasRole("ADMINISTRATOR")

                // Operational & Diagnostic Actions (Forest Operator and Administrator)
                .requestMatchers(HttpMethod.POST,
                    "/api/aws/s3-archive",
                    "/api/aws/trigger-lambda",
                    "/api/vector-clock/reset",
                    "/api/alerts/*/acknowledge"
                ).hasAnyRole("FOREST_OPERATOR", "ADMINISTRATOR")
                .requestMatchers(HttpMethod.GET, "/api/load-balancer/dispatch").hasAnyRole("FOREST_OPERATOR", "ADMINISTRATOR")

                // Read-Only Monitoring & Telemetry APIs and Pages (Viewer, Forest Operator, Administrator)
                .requestMatchers(HttpMethod.GET,
                    "/",
                    "/monitoring",
                    "/sensors",
                    "/alerts",
                    "/reports",
                    "/api/system-status",
                    "/api/nodes",
                    "/api/clusters",
                    "/api/logs",
                    "/api/events",
                    "/api/alerts",
                    "/api/elections",
                    "/api/aggregated-readings",
                    "/api/aws/status",
                    "/api/aws/s3-archives",
                    "/api/aws/lambda-invocations",
                    "/api/load-balancer/stats",
                    "/api/auth/me"
                ).hasAnyRole("VIEWER", "FOREST_OPERATOR", "ADMINISTRATOR")

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
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("FW_CENTRAL_SESSION", "FW_CENTRAL_XSRF", "JSESSIONID", "XSRF-TOKEN")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    if (isApiRequest(request)) {
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Authentication required\"}");
                    } else {
                        response.sendRedirect("/login");
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    if (isApiRequest(request)) {
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write("{\"error\":\"FORBIDDEN\",\"message\":\"Access denied for your current role\"}");
                    } else {
                        response.sendRedirect("/access-denied");
                    }
                })
            )
            // Allow H2 console frame options in development
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public org.springframework.boot.web.server.WebServerFactoryCustomizer<org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory> sessionCookieCustomizer() {
        return factory -> factory.addContextCustomizers(context -> context.setSessionCookieName("FW_CENTRAL_SESSION"));
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

