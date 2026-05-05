-- Reference schema aligned with Flyway migrations V1–V3 (manual apply or diff against Flyway).

CREATE TABLE app_users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(120) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at      TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX idx_app_users_email ON app_users (email);

CREATE TABLE quizzes (
    id                  BIGSERIAL PRIMARY KEY,
    title               VARCHAR(200) NOT NULL,
    description         VARCHAR(2000),
    created_by_user_id  BIGINT REFERENCES app_users (id) ON DELETE SET NULL,
    published           BOOL NOT NULL DEFAULT false,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL
);

CREATE TABLE questions (
    id                   BIGSERIAL PRIMARY KEY,
    quiz_id              BIGINT NOT NULL REFERENCES quizzes (id) ON DELETE CASCADE,
    text                 VARCHAR(2000) NOT NULL,
    choices_json         TEXT NOT NULL,
    correct_choice_index BIGINT NOT NULL,
    order_index          BIGINT NOT NULL
);

CREATE TABLE submissions (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES app_users (id),
    quiz_id           BIGINT NOT NULL REFERENCES quizzes (id),
    answers_json      TEXT NOT NULL,
    idempotency_key   VARCHAR(128) NULL,
    submitted_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_submission_user_quiz UNIQUE (user_id, quiz_id)
);

CREATE INDEX idx_submissions_quiz ON submissions (quiz_id);
CREATE INDEX idx_submissions_user ON submissions (user_id);
CREATE UNIQUE INDEX uk_submission_user_quiz_idem
    ON submissions (user_id, quiz_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE results (
    id             BIGSERIAL PRIMARY KEY,
    submission_id  BIGINT NOT NULL UNIQUE REFERENCES submissions (id),
    score          BIGINT NOT NULL,
    max_score      BIGINT NOT NULL,
    percentage     DOUBLE PRECISION NOT NULL,
    calculated_at  TIMESTAMPTZ NOT NULL
);
