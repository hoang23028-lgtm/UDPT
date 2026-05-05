package com.quiz.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record SubmitQuizRequest(
        @NotNull JsonNode answers,
        @Size(max = 128) String idempotencyKey,
        Instant attemptStartedAt,
        @Size(max = 200) String examPassword
) {
    public SubmitQuizRequest {
        if (idempotencyKey != null) {
            String t = idempotencyKey.trim();
            idempotencyKey = t.isEmpty() ? null : t;
        }
        if (examPassword != null) {
            String t = examPassword.trim();
            examPassword = t.isEmpty() ? null : t;
        }
    }
}
