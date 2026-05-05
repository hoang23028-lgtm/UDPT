package com.quiz.security;

import com.quiz.entity.UserRole;
import com.quiz.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("quizSecurity")
@RequiredArgsConstructor
public class QuizSecurity {

    private final QuizRepository quizRepository;

    public boolean canEditQuiz(Long quizId, Long userId) {
        if (userId == null) {
            return false;
        }
        return quizRepository.findById(quizId)
                .map(q -> q.getCreatedBy() != null && q.getCreatedBy().getId().equals(userId))
                .orElse(false);
    }

    public boolean canEditQuizForPrincipal(Long quizId, UserPrincipal principal) {
        if (principal.getRole() == UserRole.ADMIN) {
            return true;
        }
        if (principal.getRole() != UserRole.TEACHER) {
            return false;
        }
        return canEditQuiz(quizId, principal.getId());
    }
}
