package com.quiz.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EssayPointsRequest(
        @NotNull @Min(0) Integer additionalPoints
) {
}
