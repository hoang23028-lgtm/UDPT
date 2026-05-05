package com.quiz.dto;

import java.util.List;

public record QuestionResponse(
        Long id,
        String text,
        List<String> choices,
        Integer correctChoiceIndex,
        int orderIndex,
        String questionType,
        int points,
        String explanation
) {
}
