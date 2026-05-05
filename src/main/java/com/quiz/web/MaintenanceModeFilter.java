package com.quiz.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quiz.entity.UserRole;
import com.quiz.security.UserPrincipal;
import com.quiz.service.AppSettingsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * When maintenance mode is on, blocks API traffic with 503 except public status and admin users.
 */
@Component
@RequiredArgsConstructor
public class MaintenanceModeFilter extends OncePerRequestFilter {

    private final AppSettingsService appSettingsService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/") || uri.startsWith("/api/public/")) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!appSettingsService.getPublicStatus().maintenanceMode()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (isAdmin()) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", 503);
        body.put("error", "Service Unavailable");
        body.put("message", "The system is under maintenance. Please try again later.");
        body.put("maintenance", true);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal p) {
            return p.getRole() == UserRole.ADMIN;
        }
        return false;
    }
}
