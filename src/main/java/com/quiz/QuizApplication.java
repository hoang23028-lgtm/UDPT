package com.quiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.core.Ordered;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication(exclude = KafkaAutoConfiguration.class)
@EnableRetry(order = Ordered.HIGHEST_PRECEDENCE)
public class QuizApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuizApplication.class, args);
    }
}
