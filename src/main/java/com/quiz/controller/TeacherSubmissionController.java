package com.quiz.controller;

import com.quiz.dto.EssayPointsRequest;
import com.quiz.dto.ResultResponse;
import com.quiz.entity.UserRole;
import com.quiz.security.UserPrincipal;
import com.quiz.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher/submissions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class TeacherSubmissionController {

    private final QuizService quizService;

    @PostMapping("/{submissionId}/essay-points")
    public ResultResponse addEssayPoints(
            @PathVariable Long submissionId,
            Authentication authentication,
            @Valid @RequestBody EssayPointsRequest body
    ) {
        UserPrincipal p = (UserPrincipal) authentication.getPrincipal();
        boolean admin = p.getRole() == UserRole.ADMIN;
        return quizService.gradeEssayPoints(submissionId, p.getId(), admin, body.additionalPoints());
    }
}
