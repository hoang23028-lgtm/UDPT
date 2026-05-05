package com.quiz.repository;

import com.quiz.entity.ProcessedQuizSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ProcessedQuizSubmissionRepository extends JpaRepository<ProcessedQuizSubmission, Long> {

    /**
     * @return number of rows inserted (1 = first time / claimed, 0 = duplicate message)
     */
    @Modifying
    @Query(
            value = """
                    INSERT INTO processed_quiz_submissions (submission_id, processed_at)
                    VALUES (:submissionId, :processedAt)
                    ON CONFLICT (submission_id) DO NOTHING
                    """,
            nativeQuery = true
    )
    int tryClaim(@Param("submissionId") Long submissionId, @Param("processedAt") Instant processedAt);
}
