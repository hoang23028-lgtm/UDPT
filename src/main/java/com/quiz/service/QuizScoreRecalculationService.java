package com.quiz.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quiz.entity.Question;
import com.quiz.entity.QuestionType;
import com.quiz.entity.Quiz;
import com.quiz.entity.Result;
import com.quiz.entity.Submission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Recomputes MCQ portion from stored answers + current quiz definition; preserves awarded essay points.
 */
@Service
@RequiredArgsConstructor
public class QuizScoreRecalculationService {

    private final ObjectMapper objectMapper;
    private final QuizDtoMapper quizDtoMapper;

    @Transactional
    public void reconcileSubmissionScore(Submission submission, Quiz quizWithQuestions) {
        Map<Long, Object> answersByQuestionId = parseAnswersJson(submission.getAnswersJson());
        Set<Long> validIds = new HashSet<>();
        for (Question q : quizWithQuestions.getQuestions()) {
            validIds.add(q.getId());
        }
        for (Long qid : answersByQuestionId.keySet()) {
            if (!validIds.contains(qid)) {
                throw new IllegalStateException("Unknown question id in submission: " + qid);
            }
        }
        int maxPoints = (int) quizWithQuestions.getQuestions().stream().mapToLong(Question::getPoints).sum();
        int mcqScorePoints = 0;
        for (Question question : quizWithQuestions.getQuestions()) {
            if (question.getQuestionType() != QuestionType.MCQ) {
                continue;
            }
            Object raw = answersByQuestionId.get(question.getId());
            if (!(raw instanceof Number)) {
                continue;
            }
            int selected = ((Number) raw).intValue();
            int numChoices = quizDtoMapper.readChoices(question.getChoicesJson()).size();
            if (selected < 0 || selected >= numChoices) {
                throw new IllegalStateException("Invalid choice index for question " + question.getId());
            }
            if (question.getCorrectChoiceIndex() != null
                    && selected == question.getCorrectChoiceIndex().intValue()) {
                mcqScorePoints += question.getPoints();
            }
        }
        Result result = submission.getResult();
        if (result == null) {
            throw new IllegalStateException("Submission has no result row");
        }
        int essayPts = (int) result.getEssayPoints();
        int totalScore = mcqScorePoints + essayPts;
        double percentage = maxPoints == 0 ? 0.0 : Math.round(10000.0 * totalScore / maxPoints) / 100.0;
        double timeBonus = TimeBonusCalculator.computeTimeBonus(
                quizWithQuestions,
                submission.getAttemptStartedAt(),
                submission.getSubmittedAt(),
                false
        );
        double rankScore = TimeBonusCalculator.rankScore(totalScore, timeBonus);
        boolean changed = result.getScore() != (long) totalScore
                || result.getMaxScore() != (long) maxPoints
                || Double.compare(result.getPercentage(), percentage) != 0
                || Double.compare(result.getTimeBonus(), timeBonus) != 0
                || Double.compare(result.getRankScore(), rankScore) != 0;
        if (changed) {
            result.setScore(totalScore);
            result.setMaxScore(maxPoints);
            result.setPercentage(percentage);
            result.setTimeBonus(timeBonus);
            result.setRankScore(rankScore);
        }
    }

    private Map<Long, Object> parseAnswersJson(String answersJson) {
        try {
            Map<String, Object> raw = objectMapper.readValue(answersJson, new TypeReference<>() {
            });
            Map<Long, Object> map = new HashMap<>();
            for (Map.Entry<String, Object> e : raw.entrySet()) {
                map.put(Long.parseLong(e.getKey().trim()), e.getValue());
            }
            return map;
        } catch (Exception e) {
            throw new IllegalStateException("Invalid answers_json on submission", e);
        }
    }
}
