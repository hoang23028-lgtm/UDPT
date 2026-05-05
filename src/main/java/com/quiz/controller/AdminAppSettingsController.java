package com.quiz.controller;

import com.quiz.dto.AppSettingsUpdateRequest;
import com.quiz.dto.PublicStatusResponse;
import com.quiz.service.AppSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAppSettingsController {

    private final AppSettingsService appSettingsService;

    @GetMapping
    public PublicStatusResponse get() {
        return appSettingsService.getPublicStatus();
    }

    @PutMapping
    public PublicStatusResponse put(@Valid @RequestBody AppSettingsUpdateRequest request) {
        return appSettingsService.update(request);
    }
}
