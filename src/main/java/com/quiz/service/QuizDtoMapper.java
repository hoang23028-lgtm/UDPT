package com.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quiz.dto.QuestionResponse;
import com.quiz.dto.QuizDetailResponse;
import com.quiz.dto.QuizSummaryResponse;
import com.quiz.dto.UserResponse;
import com.quiz.entity.Question;
import com.quiz.entity.Quiz;
import com.quiz.entity.StudyGroup;
import com.quiz.entity.User;
import com.quiz.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class QuizDtoMapper {

    private final ObjectMapper objectMapper;

    public UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name(),
                user.getCreatedAt(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.isAccountLocked()
        );
    }

    public QuizSummaryResponse toSummary(Quiz quiz) {
        int count = quiz.getQuestions() == null ? 0 : quiz.getQuestions().size();
        boolean groupRestricted = quiz.getRestrictedToGroups() != null && !quiz.getRestrictedToGroups().isEmpty();
        boolean classOrDirect = (quiz.getAssignedClasses() != null && !quiz.getAssignedClasses().isEmpty())
                || (quiz.getAssignedStudents() != null && !quiz.getAssignedStudents().isEmpty());
        return new QuizSummaryResponse(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.isPublished(),
                count,
                groupRestricted,
                classOrDirect,
                quiz.getOpensAt(),
                quiz.getClosesAt(),
                Math.toIntExact(quiz.getMaxAttempts())
        );
    }

    public QuizDetailResponse toDetail(Quiz quiz, boolean revealSolutions) {
        List<QuestionResponse> questions = quiz.getQuestions().stream()
                .map(q -> toQuestionResponse(q, revealSolutions))
                .toList();
        Long createdBy = quiz.getCreatedBy() == null ? null : quiz.getCreatedBy().getId();
        List<Long> groupIds = quiz.getRestrictedToGroups() == null
                ? List.of()
                : quiz.getRestrictedToGroups().stream()
                .map(StudyGroup::getId)
                .sorted(Comparator.naturalOrder())
                .toList();
        List<Long> classIds = quiz.getAssignedClasses() == null
                ? List.of()
                : quiz.getAssignedClasses().stream()
                .map(c -> c.getId())
                .sorted(Comparator.naturalOrder())
                .toList();
        List<Long> studentIds = quiz.getAssignedStudents() == null
                ? List.of()
                : quiz.getAssignedStudents().stream()
                .map(User::getId)
                .sorted(Comparator.naturalOrder())
                .toList();
        boolean examPwd = quiz.getExamPasswordHash() != null && !quiz.getExamPasswordHash().isBlank();
        return new QuizDetailResponse(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                createdBy,
                quiz.isPublished(),
                questions,
                quiz.getCreatedAt(),
                quiz.getUpdatedAt(),
                quiz.getTimeLimitSeconds(),
                quiz.getTimeBonusMax(),
                groupIds,
                classIds,
                studentIds,
                quiz.getOpensAt(),
                quiz.getClosesAt(),
                Math.toIntExact(quiz.getMaxAttempts()),
                quiz.isShuffleQuestions(),
                quiz.isShuffleOptions(),
                examPwd,
                quiz.isBlockCopyPaste(),
                quiz.getMaxFullscreenExits() == null ? null : Math.toIntExact(quiz.getMaxFullscreenExits()),
                quiz.isShowAnswersToStudents()
        );
    }

    public QuestionResponse toQuestionResponse(Question question, boolean revealSolutions) {
        List<String> choices = readChoices(question.getChoicesJson());
        Integer correct = question.getCorrectChoiceIndex() == null
                ? null
                : Math.toIntExact(question.getCorrectChoiceIndex());
        String explanation = question.getExplanation();
        if (!revealSolutions) {
            correct = null;
            explanation = null;
        }
        return new QuestionResponse(
                question.getId(),
                question.getText(),
                choices,
                correct,
                Math.toIntExact(question.getOrderIndex()),
                question.getQuestionType().name(),
                Math.toIntExact(question.getPoints()),
                explanation
        );
    }

    public List<String> readChoices(String choicesJson) {
        try {
            return objectMapper.readValue(choicesJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid choices JSON in database");
        }
    }

    public String writeChoices(List<String> choices) {
        try {
            return objectMapper.writeValueAsString(choices);
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not serialize choices");
        }
    }
}
