package com.quiz.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "bank_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_user_id", nullable = false)
    private User teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(length = 200)
    private String chapter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DifficultyLevel difficulty = DifficultyLevel.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    @Builder.Default
    private QuestionType questionType = QuestionType.MCQ;

    @Column(nullable = false, length = 2000)
    private String stem;

    @Column(name = "choices_json", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String choicesJson = "[]";

    @Column(name = "correct_choice_index")
    private Long correctChoiceIndex;

    @Column(nullable = false)
    @Builder.Default
    private long points = 1;

    @Column(length = 4000)
    private String explanation;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (difficulty == null) {
            difficulty = DifficultyLevel.MEDIUM;
        }
        if (questionType == null) {
            questionType = QuestionType.MCQ;
        }
        if (choicesJson == null) {
            choicesJson = "[]";
        }
    }
}
