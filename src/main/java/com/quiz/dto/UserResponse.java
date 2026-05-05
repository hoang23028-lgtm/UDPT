package com.quiz.dto;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String displayName,
        String role,
        Instant createdAt,
        String phone,
        String avatarUrl,
        boolean accountLocked
) {
}
