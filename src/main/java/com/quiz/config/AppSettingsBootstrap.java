package com.quiz.config;

import com.quiz.entity.AppSettings;
import com.quiz.repository.AppSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Ensures singleton app_settings row exists when Flyway is disabled (e.g. tests with ddl-auto).
 */
@Component
@RequiredArgsConstructor
public class AppSettingsBootstrap {

    private final AppSettingsRepository appSettingsRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureAppSettingsRow() {
        if (appSettingsRepository.findById(AppSettings.SINGLETON_ID).isEmpty()) {
            appSettingsRepository.save(AppSettings.builder()
                    .id(AppSettings.SINGLETON_ID)
                    .maintenanceMode(false)
                    .announcementMessage(null)
                    .build());
        }
    }
}
