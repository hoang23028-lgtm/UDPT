package com.quiz.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record QuestionRequest(
        @NotBlank @Size(max = 2000) String text,
        @NotNull @Size(max = 30) List<@NotBlank @Size(max = 500) String> choices,
        Integer correctChoiceIndex,
        @NotNull @Min(0) Integer orderIndex,
        String questionType,
        @Min(1) Integer points,
        @Size(max = 4000) String explanation
) {
}
