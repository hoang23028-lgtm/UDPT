package com.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record QuizCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        boolean published,
        @NotNull @Size(min = 1) @Valid List<QuestionRequest> questions,
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
    public QuizCreateRequest {
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
