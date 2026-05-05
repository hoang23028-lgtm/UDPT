package com.quiz.config;

import com.quiz.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class JwtSecretValidation {

    private final JwtProperties jwtProperties;
    private final Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    public void validateJwtSecret() {
        String secret = jwtProperties.getSecret();
        int bytes = secret == null ? 0 : secret.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes (HS256). Provide APP_JWT_SECRET.");
        }
        if (isDockerProfileActive() && looksLikePlaceholder(secret)) {
            throw new IllegalStateException("APP_JWT_SECRET must be set to a real secret for docker profile.");
        }
    }

    private boolean isDockerProfileActive() {
        for (String p : environment.getActiveProfiles()) {
            if ("docker".equalsIgnoreCase(p)) {
                return true;
            }
        }
        return false;
    }

    private static boolean looksLikePlaceholder(String secret) {
        String s = secret.trim().toLowerCase();
        return s.isEmpty()
                || s.equals("changeme")
                || s.equals("change-me")
                || s.contains("dev-demo-secret")
                || s.contains("your-256-bit-secret");
    }
}

