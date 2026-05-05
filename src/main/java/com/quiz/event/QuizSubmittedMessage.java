package com.quiz.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Kafka payload for topic {@code quiz.submitted}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record QuizSubmittedMessage(long submissionId, long quizId, long userId) {
}
