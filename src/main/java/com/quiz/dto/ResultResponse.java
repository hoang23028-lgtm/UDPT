package com.quiz.dto;

import java.time.Instant;

public record ResultResponse(
        Long resultId,
        Long submissionId,
        Long userId,
        Long quizId,
        int score,
        int maxScore,
        double percentage,
        Instant calculatedAt,
        double timeBonus,
        double rankScore
) {
}
