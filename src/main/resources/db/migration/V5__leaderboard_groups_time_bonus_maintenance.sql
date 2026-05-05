-- Leaderboard periods use submissions.submitted_at (already present).
-- Speed bonus: quizzes.time_limit_seconds + time_bonus_max; results.time_bonus + rank_score.
-- Workspaces: study_groups, user_study_group, quiz_study_group.
-- Centralized maintenance + announcement.

ALTER TABLE quizzes
    ADD COLUMN IF NOT EXISTS time_limit_seconds INT NULL,
    ADD COLUMN IF NOT EXISTS time_bonus_max DOUBLE PRECISION NULL;

ALTER TABLE results
    ADD COLUMN IF NOT EXISTS time_bonus DOUBLE PRECISION NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS rank_score DOUBLE PRECISION NOT NULL DEFAULT 0;

UPDATE results SET rank_score = CAST(score AS DOUBLE PRECISION) WHERE rank_score = 0 AND time_bonus = 0;

ALTER TABLE submissions
    ADD COLUMN IF NOT EXISTS attempt_started_at TIMESTAMPTZ NULL;

CREATE TABLE IF NOT EXISTS study_groups (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    description     VARCHAR(1000),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_study_group (
    user_id          BIGINT NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    study_group_id   BIGINT NOT NULL REFERENCES study_groups (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, study_group_id)
);

CREATE INDEX IF NOT EXISTS idx_user_study_group_group ON user_study_group (study_group_id);

CREATE TABLE IF NOT EXISTS quiz_study_group (
    quiz_id          BIGINT NOT NULL REFERENCES quizzes (id) ON DELETE CASCADE,
    study_group_id   BIGINT NOT NULL REFERENCES study_groups (id) ON DELETE CASCADE,
    PRIMARY KEY (quiz_id, study_group_id)
);

CREATE INDEX IF NOT EXISTS idx_quiz_study_group_group ON quiz_study_group (study_group_id);

CREATE INDEX IF NOT EXISTS idx_submissions_quiz_submitted ON submissions (quiz_id, submitted_at DESC);

CREATE TABLE IF NOT EXISTS app_settings (
    id                    INT PRIMARY KEY,
    maintenance_mode      BOOL NOT NULL DEFAULT false,
    announcement_message  VARCHAR(2000)
);

INSERT INTO app_settings (id, maintenance_mode, announcement_message)
SELECT 1, false, NULL
WHERE NOT EXISTS (SELECT 1 FROM app_settings WHERE id = 1);
