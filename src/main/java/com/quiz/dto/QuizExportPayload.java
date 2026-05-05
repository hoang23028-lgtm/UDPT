package com.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Internal JSON backup format (import/export). {@link #formatVersion} must match {@link #CURRENT_FORMAT_VERSION}.
 */
public record QuizExportPayload(
        int formatVersion,
        Long sourceQuizId,
        @NotNull @Valid QuizCreateRequest quiz
) {
    public static final int CURRENT_FORMAT_VERSION = 1;
}
