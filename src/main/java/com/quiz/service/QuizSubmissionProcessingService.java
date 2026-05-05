package com.quiz.service;

import com.quiz.event.QuizSubmittedMessage;
import com.quiz.entity.Submission;
import com.quiz.repository.ProcessedQuizSubmissionRepository;
import com.quiz.repository.QuizRepository;
import com.quiz.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.CannotSerializeTransactionException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Idempotent Kafka consumer pipeline: claim row → recompute score → invalidate leaderboard cache.
 */
@Service
@RequiredArgsConstructor
public class QuizSubmissionProcessingService {

    private final ProcessedQuizSubmissionRepository processedQuizSubmissionRepository;
    private final SubmissionRepository submissionRepository;
    private final QuizRepository quizRepository;
    private final QuizScoreRecalculationService quizScoreRecalculationService;
    private final LeaderboardService leaderboardService;

    @Retryable(
            retryFor = {
                    ConcurrencyFailureException.class,
                    CannotSerializeTransactionException.class,
                    TransientDataAccessResourceException.class,
                    QueryTimeoutException.class
            },
            maxAttempts = 12,
            backoff = @Backoff(delay = 25, multiplier = 2, maxDelay = 500, random = true)
    )
    @Transactional
    public void processQuizSubmitted(QuizSubmittedMessage message) {
        int inserted = processedQuizSubmissionRepository.tryClaim(message.submissionId(), Instant.now());
        if (inserted == 0) {
            return;
        }
        Submission submission = submissionRepository.findByIdWithResult(message.submissionId())
                .orElseThrow(() -> new IllegalStateException("Submission not found: " + message.submissionId()));
        var quiz = quizRepository.findByIdWithQuestions(message.quizId())
                .orElseThrow(() -> new IllegalStateException("Quiz not found: " + message.quizId()));
        quizScoreRecalculationService.reconcileSubmissionScore(submission, quiz);
        leaderboardService.invalidateLeaderboard(message.quizId());
    }
}
