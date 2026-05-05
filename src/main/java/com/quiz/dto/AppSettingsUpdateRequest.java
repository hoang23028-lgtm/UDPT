package com.quiz.dto;

import jakarta.validation.constraints.Size;

public record AppSettingsUpdateRequest(
        Boolean maintenanceMode,
        @Size(max = 2000) String announcementMessage
) {
}
