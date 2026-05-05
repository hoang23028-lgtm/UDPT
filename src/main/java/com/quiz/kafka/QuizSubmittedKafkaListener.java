package com.quiz.kafka;

import com.quiz.event.QuizSubmittedMessage;
import com.quiz.service.QuizSubmissionProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class QuizSubmittedKafkaListener {

    private final QuizSubmissionProcessingService quizSubmissionProcessingService;

    @KafkaListener(
            topics = KafkaConfiguration.TOPIC_QUIZ_SUBMITTED,
            containerFactory = "quizSubmittedListenerContainerFactory"
    )
    public void onQuizSubmitted(QuizSubmittedMessage message) {
        log.debug("Consumed quiz.submitted submissionId={}", message.submissionId());
        quizSubmissionProcessingService.processQuizSubmitted(message);
    }
}
