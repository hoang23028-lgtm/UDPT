package com.quiz.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false, unique = true)
    private Submission submission;

    @Column(nullable = false)
    private long score;

    @Column(name = "max_score", nullable = false)
    private long maxScore;

    /**
     * 0–100 percentage, stored with one decimal in business logic rounding.
     */
    @Column(nullable = false)
    private double percentage;

    @Column(name = "time_bonus", nullable = false)
    @Builder.Default
    private double timeBonus = 0.0;

    /**
     * Total for ranking: correctness score + {@link #timeBonus}.
     */
    @Column(name = "rank_score", nullable = false)
    @Builder.Default
    private double rankScore = 0.0;

    @Column(name = "essay_points", nullable = false)
    @Builder.Default
    private long essayPoints = 0;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @PrePersist
    void prePersist() {
        if (calculatedAt == null) {
            calculatedAt = Instant.now();
        }
    }
}
