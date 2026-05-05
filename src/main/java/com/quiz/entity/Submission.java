package com.quiz.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "submissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_submission_user_quiz_attempt",
                columnNames = {"user_id", "quiz_id", "attempt_number"}
        ),
        indexes = {
                @Index(name = "idx_submissions_quiz", columnList = "quiz_id"),
                @Index(name = "idx_submissions_user", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    /**
     * JSON object: questionId (string) -> selected choice index (int).
     */
    @Column(name = "answers_json", nullable = false, columnDefinition = "TEXT")
    private String answersJson;

    /**
     * Optional client idempotency token (same user+quiz+key → same submission; safe retries).
     */
    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    /**
     * Client-reported start of attempt (ISO instant). Used with quiz time limit for speed bonus.
     */
    @Column(name = "attempt_started_at")
    private Instant attemptStartedAt;

    @Column(name = "attempt_number", nullable = false)
    @Builder.Default
    private long attemptNumber = 1;

    @OneToOne(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    private Result result;

    @PrePersist
    void prePersist() {
        if (submittedAt == null) {
            submittedAt = Instant.now();
        }
    }
}
