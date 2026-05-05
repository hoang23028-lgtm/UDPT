package com.quiz.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BankQuestionRequest(
        Long subjectId,
        @Size(max = 200) String chapter,
        String difficulty,
        String questionType,
        @NotBlank @Size(max = 2000) String stem,
        @NotNull List<@NotBlank @Size(max = 500) String> choices,
        Integer correctChoiceIndex,
        @NotNull @Min(1) Integer points,
        @Size(max = 4000) String explanation
) {
}
