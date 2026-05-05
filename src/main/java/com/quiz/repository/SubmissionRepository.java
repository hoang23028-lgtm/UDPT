package com.quiz.repository;

import com.quiz.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    boolean existsByUser_IdAndQuiz_Id(Long userId, Long quizId);

    long countByUser_IdAndQuiz_Id(Long userId, Long quizId);

    long countByUser_Id(Long userId);

    Optional<Submission> findByUser_IdAndQuiz_Id(Long userId, Long quizId);

    @Query("select coalesce(max(s.attemptNumber), 0) from Submission s where s.user.id = :userId and s.quiz.id = :quizId")
    long findMaxAttemptNumber(@Param("userId") Long userId, @Param("quizId") Long quizId);

    long countByQuiz_Id(Long quizId);

    @Query("SELECT s FROM Submission s JOIN FETCH s.result JOIN FETCH s.quiz q LEFT JOIN FETCH q.createdBy WHERE s.id = :id")
    Optional<Submission> findByIdWithResult(@Param("id") Long id);

    @Query("SELECT s FROM Submission s JOIN FETCH s.result WHERE s.user.id = :userId AND s.quiz.id = :quizId")
    Optional<Submission> findByUser_IdAndQuiz_IdWithResult(
            @Param("userId") Long userId,
            @Param("quizId") Long quizId
    );

    @Query("SELECT s FROM Submission s JOIN FETCH s.result JOIN FETCH s.quiz q WHERE s.user.id = :userId ORDER BY s.submittedAt DESC")
    List<Submission> findByUser_IdWithResultAndQuiz(@Param("userId") Long userId);

    @Query(
            "SELECT s FROM Submission s JOIN FETCH s.result "
                    + "WHERE s.user.id = :userId AND s.quiz.id = :quizId AND s.idempotencyKey = :idempotencyKey"
    )
    Optional<Submission> findByUser_IdAndQuiz_IdAndIdempotencyKey(
            @Param("userId") Long userId,
            @Param("quizId") Long quizId,
            @Param("idempotencyKey") String idempotencyKey
    );
}
