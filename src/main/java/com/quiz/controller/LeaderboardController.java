package com.quiz.controller;

import com.quiz.dto.LeaderboardEntryResponse;
import com.quiz.exception.ApiException;
import com.quiz.model.LeaderboardPeriod;
import com.quiz.service.LeaderboardService;
import com.quiz.service.QuizAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;
    private final QuizAccessService quizAccessService;

    @GetMapping("/{quizId}/leaderboard")
    public List<LeaderboardEntryResponse> getLeaderboard(
            @PathVariable Long quizId,
            @RequestParam(required = false, defaultValue = "ALL") String period,
            Authentication authentication
    ) {
        LeaderboardPeriod p = LeaderboardPeriod.fromParam(period);
        Long viewerUserId = currentUserId(authentication);
        if (!quizAccessService.canViewPublishedQuiz(quizId, viewerUserId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Quiz not found");
        }
        return leaderboardService.getLeaderboard(quizId, p);
    }

    private static Long currentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof com.quiz.security.UserPrincipal up) {
            return up.getId();
        }
        return null;
    }
}
