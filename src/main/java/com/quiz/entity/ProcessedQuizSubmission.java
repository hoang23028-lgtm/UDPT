package com.quiz.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "processed_quiz_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedQuizSubmission {

    @Id
    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
