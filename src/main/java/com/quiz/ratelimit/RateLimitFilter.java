package com.quiz.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final RedisFixedWindowRateLimiter limiter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method) && "/api/auth/login".equals(path)) {
            return false;
        }
        if ("POST".equalsIgnoreCase(method) && path != null && path.matches("^/api/quizzes/\\d+/submit$")) {
            return false;
        }
        return true;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        RateLimitProperties.EndpointLimit limit;
        String keyPrefix;
        if ("POST".equalsIgnoreCase(method) && "/api/auth/login".equals(path)) {
            limit = properties.getLogin();
            keyPrefix = "rl:login:";
        } else {
            limit = properties.getSubmit();
            keyPrefix = "rl:submit:";
        }

        String clientKey = clientKey(request);
        RedisFixedWindowRateLimiter.RateLimitDecision decision = limiter.allow(
                keyPrefix + clientKey,
                limit.getWindowSeconds(),
                limit.getMaxRequests()
        );
        if (!decision.allowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            if (decision.retryAfterSeconds() != null) {
                response.setHeader("Retry-After", decision.retryAfterSeconds().toString());
            }
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":429,\"message\":\"Too many requests\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static String clientKey(HttpServletRequest request) {
        // Basic best-effort: use X-Forwarded-For first when behind Nginx.
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String first = xff.split(",")[0].trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        String ip = request.getRemoteAddr();
        if (ip == null || ip.isBlank()) {
            ip = "unknown";
        }
        return ip;
    }
}

