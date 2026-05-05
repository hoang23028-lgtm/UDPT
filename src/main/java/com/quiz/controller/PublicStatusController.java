package com.quiz.controller;

import com.quiz.dto.PublicStatusResponse;
import com.quiz.service.AppSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicStatusController {

    private final AppSettingsService appSettingsService;

    @GetMapping("/status")
    public PublicStatusResponse status() {
        return appSettingsService.getPublicStatus();
    }
}
