package com.quiz.repository;

import com.quiz.entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResultRepository extends JpaRepository<Result, Long> {

    Optional<Result> findBySubmissionId(Long submissionId);

    @Query(
            value = """
                    WITH ranked AS (
                        SELECT u.id AS uid, u.display_name AS dname, r.score AS sc, r.max_score AS ms,
                               r.percentage AS pct, r.time_bonus AS tb, r.rank_score AS rs, s.submitted_at AS sa,
                               ROW_NUMBER() OVER (
                                   PARTITION BY u.id
                                   ORDER BY r.rank_score DESC, r.score DESC, s.submitted_at ASC
                               ) AS rn
                        FROM results r
                        INNER JOIN submissions s ON r.submission_id = s.id
                        INNER JOIN app_users u ON s.user_id = u.id
                        WHERE s.quiz_id = :quizId
                          AND s.submitted_at >= CAST(:submittedFrom AS TIMESTAMPTZ)
                    )
                    SELECT uid, dname, sc, ms, pct, tb, rs
                    FROM ranked
                    WHERE rn = 1
                    ORDER BY rs DESC, sc DESC, pct DESC
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<Object[]> findLeaderboardRows(
            @Param("quizId") Long quizId,
            @Param("submittedFrom") java.time.Instant submittedFrom,
            @Param("limit") int limit
    );
}
