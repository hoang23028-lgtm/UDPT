package com.quiz.controller;

import com.quiz.dto.QuizDetailResponse;
import com.quiz.dto.QuizExportPayload;
import com.quiz.exception.ApiException;
import com.quiz.security.UserPrincipal;
import com.quiz.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/quizzes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminQuizImportExportController {

    private final QuizService quizService;

    @PostMapping("/import")
    public ResponseEntity<QuizDetailResponse> importQuiz(
            Authentication authentication,
            @Valid @RequestBody QuizExportPayload payload
    ) {
        if (payload.formatVersion() != QuizExportPayload.CURRENT_FORMAT_VERSION) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported formatVersion: " + payload.formatVersion());
        }
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        QuizDetailResponse created = quizService.createQuiz(payload.quiz(), principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}/export")
    public QuizExportPayload export(@PathVariable Long id) {
        return quizService.exportQuizBackup(id);
    }
}
