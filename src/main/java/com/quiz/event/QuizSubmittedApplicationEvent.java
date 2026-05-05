package com.quiz.event;

/**
 * Published after a quiz submission is persisted; {@link com.quiz.kafka.QuizSubmittedKafkaPublisher}
 * forwards to Kafka after transaction commit.
 */
public record QuizSubmittedApplicationEvent(long submissionId, long quizId, long userId) {
}
