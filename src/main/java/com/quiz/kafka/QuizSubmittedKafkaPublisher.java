package com.quiz.kafka;

import com.quiz.event.QuizSubmittedApplicationEvent;
import com.quiz.event.QuizSubmittedMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class QuizSubmittedKafkaPublisher {

    private final KafkaTemplate<String, QuizSubmittedMessage> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAfterCommit(QuizSubmittedApplicationEvent event) {
        QuizSubmittedMessage payload = new QuizSubmittedMessage(
                event.submissionId(),
                event.quizId(),
                event.userId()
        );
        kafkaTemplate.send(KafkaConfiguration.TOPIC_QUIZ_SUBMITTED, String.valueOf(event.submissionId()), payload);
    }
}
