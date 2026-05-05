package com.quiz.entity;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "quizzes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Column(nullable = false)
    @Builder.Default
    private boolean published = false;

    @BatchSize(size = 32)
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Optional wall-clock limit for the attempt when {@link #timeBonusMax} is used; seconds.
     */
    @Column(name = "time_limit_seconds")
    private Long timeLimitSeconds;

    /**
     * Maximum extra points (speed bonus) added on top of correct-answer score when within time limit.
     */
    @Column(name = "time_bonus_max")
    private Double timeBonusMax;

    /**
     * Empty = published quiz is visible to everyone (legacy). Non-empty = only members of these groups.
     */
    @ManyToMany
    @JoinTable(
            name = "quiz_study_group",
            joinColumns = @JoinColumn(name = "quiz_id"),
            inverseJoinColumns = @JoinColumn(name = "study_group_id")
    )
    @Builder.Default
    private Set<StudyGroup> restrictedToGroups = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "quiz_school_class",
            joinColumns = @JoinColumn(name = "quiz_id"),
            inverseJoinColumns = @JoinColumn(name = "school_class_id")
    )
    @Builder.Default
    private Set<SchoolClass> assignedClasses = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "quiz_direct_assignee",
            joinColumns = @JoinColumn(name = "quiz_id"),
            inverseJoinColumns = @JoinColumn(name = "student_user_id")
    )
    @Builder.Default
    private Set<User> assignedStudents = new HashSet<>();

    @Column(name = "opens_at")
    private Instant opensAt;

    @Column(name = "closes_at")
    private Instant closesAt;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private long maxAttempts = 1;

    @Column(name = "shuffle_questions", nullable = false)
    @Builder.Default
    private boolean shuffleQuestions = false;

    @Column(name = "shuffle_options", nullable = false)
    @Builder.Default
    private boolean shuffleOptions = false;

    @Column(name = "exam_password_hash", length = 255)
    private String examPasswordHash;

    @Column(name = "block_copy_paste", nullable = false)
    @Builder.Default
    private boolean blockCopyPaste = false;

    @Column(name = "max_fullscreen_exits")
    private Long maxFullscreenExits;

    @Column(name = "show_answers_to_students", nullable = false)
    @Builder.Default
    private boolean showAnswersToStudents = false;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void addQuestion(Question question) {
        questions.add(question);
        question.setQuiz(this);
    }

    public void clearQuestions() {
        questions.forEach(q -> q.setQuiz(null));
        questions.clear();
    }
}
