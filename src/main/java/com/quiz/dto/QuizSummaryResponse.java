package com.quiz.dto;

import java.time.Instant;

public record QuizSummaryResponse(
        Long id,
        String title,
        String description,
        boolean published,
        int questionCount,
        boolean groupRestricted,
        boolean classOrDirectRestricted,
        Instant opensAt,
        Instant closesAt,
        int maxAttempts
) {
}
