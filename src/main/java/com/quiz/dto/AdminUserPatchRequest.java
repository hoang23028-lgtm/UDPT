package com.quiz.dto;

import jakarta.validation.constraints.Size;

public record AdminUserPatchRequest(
        Boolean accountLocked,
        @Size(max = 32) String role
) {
}
