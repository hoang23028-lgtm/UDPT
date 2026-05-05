package com.quiz.repository;

import com.quiz.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    @Query(
            "select distinct q from Quiz q "
                    + "left join fetch q.questions "
                    + "left join fetch q.restrictedToGroups "
                    + "left join fetch q.assignedClasses "
                    + "left join fetch q.assignedStudents "
                    + "where q.id = :id"
    )
    Optional<Quiz> findByIdWithQuestions(@Param("id") Long id);

    @Query(
            value = """
                    SELECT q.id FROM quizzes q
                    WHERE q.published = true
                    AND (
                        (
                            NOT EXISTS (SELECT 1 FROM quiz_study_group qsg WHERE qsg.quiz_id = q.id)
                            AND NOT EXISTS (SELECT 1 FROM quiz_school_class qsc WHERE qsc.quiz_id = q.id)
                            AND NOT EXISTS (SELECT 1 FROM quiz_direct_assignee qda WHERE qda.quiz_id = q.id)
                        )
                        OR (
                            CAST(:userId AS BIGINT) IS NOT NULL
                            AND (
                                EXISTS (
                                    SELECT 1 FROM quiz_study_group qsg
                                    INNER JOIN user_study_group usg ON usg.study_group_id = qsg.study_group_id
                                    WHERE qsg.quiz_id = q.id AND usg.user_id = :userId
                                )
                                OR EXISTS (
                                    SELECT 1 FROM quiz_school_class qsc
                                    INNER JOIN class_roster cr ON cr.school_class_id = qsc.school_class_id
                                    WHERE qsc.quiz_id = q.id AND cr.student_user_id = :userId
                                )
                                OR EXISTS (
                                    SELECT 1 FROM quiz_direct_assignee qda
                                    WHERE qda.quiz_id = q.id AND qda.student_user_id = :userId
                                )
                            )
                        )
                    )
                    ORDER BY q.id ASC
                    """,
            nativeQuery = true
    )
    List<Long> findPublishedVisibleQuizIds(@Param("userId") Long userId);

    List<Quiz> findByCreatedBy_IdOrderByIdAsc(Long createdByUserId);

    long countByCreatedBy_Id(Long createdByUserId);
}
