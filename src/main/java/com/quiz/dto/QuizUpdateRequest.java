package com.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * Null fields mean "leave unchanged". For {@code questions}, null means no change;
 * an empty list is rejected (use DELETE quiz instead or keep at least one question).
 */
public record QuizUpdateRequest(
        @Size(max = 200) String title,
        @Size(max = 2000) String description,
        Boolean published,
        @Valid List<QuestionRequest> questions,
        List<Long> studyGroupIds,
        Long timeLimitSeconds,
        Double timeBonusMax,
        List<Long> assignedClassIds,
        List<Long> assignedStudentUserIds,
        Instant opensAt,
        Instant closesAt,
        Integer maxAttempts,
        Boolean shuffleQuestions,
        Boolean shuffleOptions,
        String examPassword,
        Boolean blockCopyPaste,
        Integer maxFullscreenExits,
        Boolean showAnswersToStudents
) {
    public QuizUpdateRequest {
        if (studyGroupIds != null) {
            studyGroupIds = List.copyOf(studyGroupIds);
        }
        if (assignedClassIds != null) {
            assignedClassIds = List.copyOf(assignedClassIds);
        }
        if (assignedStudentUserIds != null) {
            assignedStudentUserIds = List.copyOf(assignedStudentUserIds);
        }
    }
}
