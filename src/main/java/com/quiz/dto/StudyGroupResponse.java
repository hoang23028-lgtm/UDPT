package com.quiz.dto;

import java.time.Instant;

public record StudyGroupResponse(
        Long id,
        String name,
        String description,
        Instant createdAt
) {
}
