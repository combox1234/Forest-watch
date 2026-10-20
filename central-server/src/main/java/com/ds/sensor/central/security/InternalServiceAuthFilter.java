package com.ds.sensor.central.security;

import com.ds.sensor.shared.model.SecurityConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class InternalServiceAuthFilter extends OncePerRequestFilter {

    @Value("${forestwatch.security.service-key:" + SecurityConstants.DEFAULT_SERVICE_KEY + "}")
    private String configuredServiceKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String headerKey = request.getHeader(SecurityConstants.SERVICE_HEADER_NAME);

        if (configuredServiceKey != null && !configuredServiceKey.trim().isEmpty() && configuredServiceKey.equals(headerKey)) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    "sensor-internal-service",
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(SecurityConstants.ROLE_SENSOR_SERVICE))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
