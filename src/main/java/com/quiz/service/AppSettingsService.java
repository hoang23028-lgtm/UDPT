package com.quiz.service;

import com.quiz.config.CacheConfiguration;
import com.quiz.dto.AppSettingsUpdateRequest;
import com.quiz.dto.PublicStatusResponse;
import com.quiz.entity.AppSettings;
import com.quiz.repository.AppSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppSettingsService {

    private final AppSettingsRepository appSettingsRepository;

    @Cacheable(cacheNames = CacheConfiguration.APP_SETTINGS_CACHE, key = "'singleton'")
    @Transactional(readOnly = true)
    public PublicStatusResponse getPublicStatus() {
        AppSettings s = appSettingsRepository.findById(AppSettings.SINGLETON_ID).orElseThrow();
        return new PublicStatusResponse(s.isMaintenanceMode(), s.getAnnouncementMessage());
    }

    @CacheEvict(cacheNames = CacheConfiguration.APP_SETTINGS_CACHE, key = "'singleton'")
    @Transactional
    public PublicStatusResponse update(AppSettingsUpdateRequest request) {
        AppSettings s = appSettingsRepository.findById(AppSettings.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("app_settings row missing"));
        if (request.maintenanceMode() != null) {
            s.setMaintenanceMode(request.maintenanceMode());
        }
        if (request.announcementMessage() != null) {
            String msg = request.announcementMessage().trim();
            s.setAnnouncementMessage(msg.isEmpty() ? null : msg);
        }
        appSettingsRepository.save(s);
        return new PublicStatusResponse(s.isMaintenanceMode(), s.getAnnouncementMessage());
    }
}
