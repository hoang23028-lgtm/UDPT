-- School exam model: roles (STUDENT/TEACHER), master data, question bank, quiz scheduling & assignments, multi-attempt.

UPDATE app_users SET role = 'STUDENT' WHERE role = 'USER';
UPDATE app_users SET role = 'TEACHER' WHERE role = 'CREATOR';

ALTER TABLE app_users ADD COLUMN IF NOT EXISTS phone VARCHAR(32);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS account_locked BOOL NOT NULL DEFAULT false;

CREATE TABLE IF NOT EXISTS school_years (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    starts_on   DATE NULL,
    ends_on     DATE NULL
);

CREATE TABLE IF NOT EXISTS grade_levels (
    id     BIGSERIAL PRIMARY KEY,
    code   VARCHAR(32) NOT NULL,
    name   VARCHAR(120) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_grade_levels_code ON grade_levels (code);

CREATE TABLE IF NOT EXISTS subjects (
    id   BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(200) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_subjects_code ON subjects (code);

CREATE TABLE IF NOT EXISTS school_classes (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(120) NOT NULL,
    school_year_id  BIGINT NULL REFERENCES school_years (id) ON DELETE SET NULL,
    grade_level_id  BIGINT NULL REFERENCES grade_levels (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_school_classes_year ON school_classes (school_year_id);

CREATE TABLE IF NOT EXISTS class_roster (
    id                BIGSERIAL PRIMARY KEY,
    school_class_id   BIGINT NOT NULL REFERENCES school_classes (id) ON DELETE CASCADE,
    student_user_id   BIGINT NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    UNIQUE (school_class_id, student_user_id)
);

CREATE INDEX IF NOT EXISTS idx_class_roster_student ON class_roster (student_user_id);

CREATE TABLE IF NOT EXISTS teacher_assignments (
    id                BIGSERIAL PRIMARY KEY,
    teacher_user_id   BIGINT NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    subject_id        BIGINT NOT NULL REFERENCES subjects (id) ON DELETE CASCADE,
    school_class_id   BIGINT NOT NULL REFERENCES school_classes (id) ON DELETE CASCADE,
    UNIQUE (teacher_user_id, subject_id, school_class_id)
);

CREATE TABLE IF NOT EXISTS bank_questions (
    id                   BIGSERIAL PRIMARY KEY,
    teacher_user_id      BIGINT NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    subject_id           BIGINT NULL REFERENCES subjects (id) ON DELETE SET NULL,
    chapter              VARCHAR(200),
    difficulty           VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    question_type        VARCHAR(20) NOT NULL DEFAULT 'MCQ',
    stem                 VARCHAR(2000) NOT NULL,
    choices_json         TEXT NOT NULL,
    correct_choice_index BIGINT NULL,
    points               INT NOT NULL DEFAULT 1,
    explanation          VARCHAR(4000),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_bank_questions_teacher ON bank_questions (teacher_user_id);

ALTER TABLE quizzes
    ADD COLUMN IF NOT EXISTS opens_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS closes_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS max_attempts INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS shuffle_questions BOOL NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS shuffle_options BOOL NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS exam_password_hash VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS block_copy_paste BOOL NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS max_fullscreen_exits INT NULL,
    ADD COLUMN IF NOT EXISTS show_answers_to_students BOOL NOT NULL DEFAULT false;

ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS question_type VARCHAR(20) NOT NULL DEFAULT 'MCQ',
    ADD COLUMN IF NOT EXISTS points INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS explanation VARCHAR(4000) NULL;

ALTER TABLE questions ALTER COLUMN correct_choice_index DROP NOT NULL;

CREATE TABLE IF NOT EXISTS quiz_school_class (
    quiz_id           BIGINT NOT NULL REFERENCES quizzes (id) ON DELETE CASCADE,
    school_class_id   BIGINT NOT NULL REFERENCES school_classes (id) ON DELETE CASCADE,
    PRIMARY KEY (quiz_id, school_class_id)
);

CREATE TABLE IF NOT EXISTS quiz_direct_assignee (
    quiz_id           BIGINT NOT NULL REFERENCES quizzes (id) ON DELETE CASCADE,
    student_user_id   BIGINT NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    PRIMARY KEY (quiz_id, student_user_id)
);

-- CockroachDB: inline UNIQUE is backed by an index; use DROP INDEX, not ALTER DROP CONSTRAINT.
DROP INDEX IF EXISTS uk_submission_user_quiz CASCADE;

ALTER TABLE submissions ADD COLUMN IF NOT EXISTS attempt_number INT NOT NULL DEFAULT 1;

UPDATE submissions SET attempt_number = 1 WHERE attempt_number IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_submission_user_quiz_attempt ON submissions (user_id, quiz_id, attempt_number);

CREATE UNIQUE INDEX IF NOT EXISTS uk_submission_idempotency
    ON submissions (user_id, quiz_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

ALTER TABLE results ADD COLUMN IF NOT EXISTS essay_points INT NOT NULL DEFAULT 0;
