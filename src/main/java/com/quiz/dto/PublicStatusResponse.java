package com.quiz.dto;

public record PublicStatusResponse(
        boolean maintenanceMode,
        String announcementMessage
) {
}
