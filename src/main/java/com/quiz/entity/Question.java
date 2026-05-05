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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(nullable = false, length = 2000)
    private String text;

    /**
     * JSON array of choice strings, e.g. ["Paris","London","Berlin"].
     */
    @Column(name = "choices_json", nullable = false, columnDefinition = "TEXT")
    private String choicesJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    @Builder.Default
    private QuestionType questionType = QuestionType.MCQ;

    @Column(nullable = false)
    @Builder.Default
    private long points = 1;

    @Column(length = 4000)
    private String explanation;

    @Column(name = "correct_choice_index")
    private Long correctChoiceIndex;

    @Column(name = "order_index", nullable = false)
    private long orderIndex;
}
