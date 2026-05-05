package com.quiz.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegistrationRequest(
        @Email @NotBlank @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 120) String displayName,
        /**
         * Allowed values: STUDENT, TEACHER. If omitted, defaults to STUDENT.
         */
        @Size(max = 20) String role
) {
}
