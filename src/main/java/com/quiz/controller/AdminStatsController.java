package com.quiz.controller;

import com.quiz.repository.QuizRepository;
import com.quiz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    private final UserRepository userRepository;
    private final QuizRepository quizRepository;

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return Map.of(
                "userCount", userRepository.count(),
                "quizCount", quizRepository.count()
        );
    }
}
