package com.quiz.dto;

public record LeaderboardEntryResponse(
        int rank,
        Long userId,
        String displayName,
        int score,
        int maxScore,
        double percentage,
        double timeBonus,
        double rankScore
) {
}
