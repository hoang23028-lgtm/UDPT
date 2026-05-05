-- Idempotency for Kafka consumer: one successful processing row per submission.

CREATE TABLE processed_quiz_submissions (
    submission_id BIGINT NOT NULL PRIMARY KEY REFERENCES submissions (id) ON DELETE CASCADE,
    processed_at  TIMESTAMPTZ NOT NULL
);
