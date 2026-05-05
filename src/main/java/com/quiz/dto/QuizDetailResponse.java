package com.quiz.dto;

import java.time.Instant;
import java.util.List;

public record QuizDetailResponse(
        Long id,
        String title,
        String description,
        Long createdByUserId,
        boolean published,
        List<QuestionResponse> questions,
        Instant createdAt,
        Instant updatedAt,
        Long timeLimitSeconds,
        Double timeBonusMax,
        List<Long> studyGroupIds,
        List<Long> assignedClassIds,
        List<Long> assignedStudentUserIds,
        Instant opensAt,
        Instant closesAt,
        int maxAttempts,
        boolean shuffleQuestions,
        boolean shuffleOptions,
        boolean examPasswordRequired,
        boolean blockCopyPaste,
        Integer maxFullscreenExits,
        boolean showAnswersToStudents
) {
}
