package com.quiz.dto;

import java.time.Instant;

public record MySubmissionSummaryResponse(
        String submissionId,
        String quizId,
        String quizTitle,
        boolean quizPublished,
        long attemptNumber,
        Instant submittedAt,
        int score,
        int maxScore,
        double percentage,
        double timeBonus,
        double rankScore
) {
}

