-- Client-supplied idempotency key (header or body) for safe retries after timeouts / app crashes.
-- Partial unique index: multiple NULL keys allowed (legacy rows); non-null keys unique per (user, quiz).

ALTER TABLE submissions
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128) NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_submission_user_quiz_idem
    ON submissions (user_id, quiz_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
