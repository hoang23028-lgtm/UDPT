package com.quiz.service;

import com.quiz.entity.Quiz;
import com.quiz.entity.SchoolClass;
import com.quiz.entity.User;
import com.quiz.repository.ClassRosterRepository;
import com.quiz.repository.QuizRepository;
import com.quiz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizAccessService {

    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final ClassRosterRepository classRosterRepository;

    @Transactional(readOnly = true)
    public boolean canViewPublishedQuiz(Long quizId, Long viewerUserId) {
        Quiz q = quizRepository.findById(quizId).orElse(null);
        if (q == null || !q.isPublished()) {
            return false;
        }
        return canViewPublished(q, viewerUserId);
    }

    @Transactional(readOnly = true)
    public boolean canViewPublished(Quiz quiz, Long viewerUserId) {
        boolean hasGroup = quiz.getRestrictedToGroups() != null && !quiz.getRestrictedToGroups().isEmpty();
        boolean hasClass = quiz.getAssignedClasses() != null && !quiz.getAssignedClasses().isEmpty();
        boolean hasDirect = quiz.getAssignedStudents() != null && !quiz.getAssignedStudents().isEmpty();
        if (!hasGroup && !hasClass && !hasDirect) {
            return true;
        }
        if (viewerUserId == null) {
            return false;
        }
        boolean ok = false;
        if (hasGroup) {
            User viewer = userRepository.findByIdWithStudyGroups(viewerUserId).orElse(null);
            if (viewer != null) {
                ok = ok || viewer.getStudyGroups().stream().anyMatch(quiz.getRestrictedToGroups()::contains);
            }
        }
        if (hasClass) {
            Set<Long> classIds = quiz.getAssignedClasses().stream().map(SchoolClass::getId).collect(Collectors.toSet());
            ok = ok || classRosterRepository.existsByStudent_IdAndSchoolClass_IdIn(viewerUserId, classIds);
        }
        if (hasDirect) {
            ok = ok || quiz.getAssignedStudents().stream().anyMatch(u -> u.getId().equals(viewerUserId));
        }
        return ok;
    }

    @Transactional(readOnly = true)
    public boolean canViewDraftAsAuthorOrAdmin(Quiz quiz, Long viewerUserId, boolean isAdmin) {
        if (isAdmin) {
            return true;
        }
        if (viewerUserId == null) {
            return false;
        }
        return quiz.getCreatedBy() != null && quiz.getCreatedBy().getId().equals(viewerUserId);
    }
}
