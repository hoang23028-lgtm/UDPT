package com.quiz.dto;

import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @Size(max = 120) String displayName,
        @Size(max = 32) String phone,
        @Size(max = 500) String avatarUrl
) {
}
