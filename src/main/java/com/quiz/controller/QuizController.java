package com.quiz.controller;

import com.quiz.dto.QuizCreateRequest;
import com.quiz.dto.QuizDetailResponse;
import com.quiz.dto.QuizSummaryResponse;
import com.quiz.dto.QuizUpdateRequest;
import com.quiz.dto.ResultResponse;
import com.quiz.dto.SubmitQuizRequest;
import com.quiz.entity.UserRole;
import com.quiz.security.UserPrincipal;
import com.quiz.service.QuizService;
import com.quiz.util.SubmitIdempotencyKeys;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
@Validated
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @GetMapping
    public List<QuizSummaryResponse> listQuizzes(Authentication authentication) {
        return quizService.listQuizzes(
                currentUserId(authentication),
                hasRole(authentication, UserRole.ADMIN),
                currentRole(authentication)
        );
    }

    @GetMapping("/{id}")
    public QuizDetailResponse getQuiz(@PathVariable Long id, Authentication authentication) {
        return quizService.getQuiz(
                id,
                currentUserId(authentication),
                hasRole(authentication, UserRole.ADMIN)
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<QuizDetailResponse> createQuiz(
            Authentication authentication,
            @Valid @RequestBody QuizCreateRequest request
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        QuizDetailResponse body = quizService.createQuiz(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TEACHER') and @quizSecurity.canEditQuizForPrincipal(#id, authentication.principal))")
    public QuizDetailResponse updateQuiz(@PathVariable Long id, @Valid @RequestBody QuizUpdateRequest request) {
        return quizService.updateQuiz(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TEACHER') and @quizSecurity.canEditQuizForPrincipal(#id, authentication.principal))")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuiz(@PathVariable Long id) {
        quizService.deleteQuiz(id);
    }

    @PostMapping("/{id}/submit")
    public ResultResponse submitQuiz(
            @PathVariable Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            Authentication authentication,
            @Valid @RequestBody SubmitQuizRequest request
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String idempotencyKey = SubmitIdempotencyKeys.resolve(idempotencyKeyHeader, request.idempotencyKey());
        return quizService.submitQuiz(id, principal.getId(), request, idempotencyKey);
    }

    private static Long currentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof UserPrincipal up) {
            return up.getId();
        }
        return null;
    }

    private static UserRole currentRole(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof UserPrincipal up) {
            return up.getRole();
        }
        return null;
    }

    private static boolean hasRole(Authentication authentication, UserRole role) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_" + role.name()));
    }
}
