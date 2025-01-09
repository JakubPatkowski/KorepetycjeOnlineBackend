-- Tworzenie schematu 'demo'
CREATE SCHEMA IF NOT EXISTS demo;

-- Ustawienia kodowania znaków oraz strefy czasowej
SET
CLIENT_ENCODING TO 'UTF8';
SET
TIMEZONE TO 'Europe/Warsaw';

-- Używanie schematu 'demo'
SET
search_path TO demo;

-- Usuwanie tabel jeśli istnieją
DROP TABLE IF EXISTS users, user_profiles, refresh_tokens, verification_tokens, courses, chapters, subchapters, content_items, files, points_offers, purchased_courses, roles, reviews, teacher_profiles, tasks, demo.login_attempts, payment_history CASCADE;

-- Tworzenie tabeli Users
CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(60)     NOT NULL,
    points        INTEGER   DEFAULT 0,
    verified      BOOLEAN   DEFAULT FALSE,
    blocked       BOOLEAN   DEFAULT FALSE,
    mfa           BOOLEAN   DEFAULT FALSE
);

-- Tworzenie tabeli User_profile
CREATE TABLE user_profiles
(
    id               BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    user_id          BIGINT    NOT NULL REFERENCES users (id),
    description      VARCHAR(500) DEFAULT NULL,
    created_at       DATE         DEFAULT CURRENT_TIMESTAMP,
    picture          BYTEA        DEFAULT NULL,
    mime_type        VARCHAR(255) DEFAULT NULL,
    badges_visible   BOOLEAN    DEFAULT TRUE
);

CREATE TABLE refresh_tokens
(
    id         BIGSERIAL PRIMARY KEY,
    ref_token  VARCHAR(100) NOT NULL,
    user_id    BIGINT    NOT NULL REFERENCES users (id),
    ip         VARCHAR(200) NOT NULL,
    expiration TIMESTAMP    NOT NULL,
    created_at TIMESTAMP    NOT NULL
);

CREATE TABLE verification_tokens
(
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     BIGINT NOT NULL REFERENCES  users (id),
    expiration  TIMESTAMP NOT NULL,
    token_type  VARCHAR(50) NOT NULL,
    new_email   VARCHAR(255)

);

CREATE TABLE courses
(
    id  BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    banner BYTEA,
    mime_type VARCHAR(255),
    review DECIMAL(2,1),
    price DECIMAL(10,2) NOT NULL,
    duration DECIMAL(10,2),
    user_id BIGINT REFERENCES users(id),
    tags  VARCHAR(255)[],
    review_number INTEGER DEFAULT 0,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chapters
(
    id        BIGSERIAL PRIMARY KEY,
    course_id BIGINT REFERENCES courses(id) ON DELETE CASCADE,
    name      VARCHAR(255) NOT NULL,
    "order"     INTEGER NOT NULL,
    review    DECIMAL(2,1) DEFAULT  0,
    review_number INTEGER DEFAULT 0
);

CREATE TABLE subchapters (
    id BIGSERIAL PRIMARY KEY,
    chapter_id BIGINT REFERENCES chapters(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    "order" INTEGER NOT NULL
);

CREATE TABLE content_items (
    id BIGSERIAL PRIMARY KEY,
    subchapter_id BIGINT REFERENCES subchapters(id),
    type VARCHAR(50) NOT NULL,
    "order" INTEGER NOT NULL,
--     Text specific fields
    text TEXT,
    font_size VARCHAR(20),  -- 'small', 'medium', 'large'
    bolder BOOLEAN,
    text_color VARCHAR(255),
    italics BOOLEAN,
    underline BOOLEAN,
    -- File specific fields
    file BYTEA,
    mime_type VARCHAR(255),
    quiz_data JSONB, -- Stores quiz questions and answers in this format:


    CHECK (
        (type = 'text' AND text IS NOT NULL) OR
        (type IN ('video', 'image') AND file IS NOT NULL) OR
        (type = 'quiz' AND quiz_data IS NOT NULL)
        )

);

CREATE TABLE points_offers (
    id BIGSERIAL PRIMARY KEY,
    points INTEGER NOT NULL,
    price INTEGER NOT NULL,
    offer_type VARCHAR(10) NOT NULL DEFAULT 'BUY'
);

CREATE TABLE purchased_courses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    course_id BIGINT REFERENCES courses(id),
    purchase_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    points_spent INTEGER NOT NULL,
    UNIQUE(user_id, course_id)
);

CREATE TABLE roles (
                            id BIGSERIAL PRIMARY KEY,
                            user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
                            role VARCHAR(50) NOT NULL,
                            UNIQUE(user_id, role)
);

CREATE TABLE reviews (
                         id BIGSERIAL PRIMARY KEY,
                         user_id BIGINT REFERENCES users(id),
                         target_id BIGINT NOT NULL,
                         target_type VARCHAR(50) NOT NULL CHECK (target_type IN ('COURSE', 'CHAPTER', 'TEACHER')),
                         rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
                         content TEXT,
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP,
                         UNIQUE(user_id, target_id, target_type)
);

CREATE TABLE teacher_profiles (
                                  id BIGSERIAL PRIMARY KEY,
                                  user_id BIGINT NOT NULL REFERENCES users(id),
                                  review DECIMAL(2,1) CHECK (review >= 0 AND review <= 5),
                                  review_number INTEGER DEFAULT 0,
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  CONSTRAINT unique_teacher_profile UNIQUE (user_id)
);

CREATE TABLE demo.tasks (
                            id BIGSERIAL PRIMARY KEY,
                            title VARCHAR(255) NOT NULL,
                            content TEXT NOT NULL,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            end_date TIMESTAMP NOT NULL,
                            file BYTEA,
                            mime_type VARCHAR(255),
                            price INTEGER NOT NULL,
                            solution_time_minutes INTEGER NOT NULL,
                            is_public BOOLEAN DEFAULT true,
                            is_active BOOLEAN DEFAULT true,
                            student_id BIGINT NOT NULL REFERENCES users(id),
                            assigned_teacher_id BIGINT REFERENCES users(id),
                            assigned_at TIMESTAMP,
                            solution_deadline TIMESTAMP,
                            status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                            CHECK (status IN ('OPEN', 'ASSIGNED', 'COMPLETED', 'EXPIRED'))
);

CREATE TABLE IF NOT EXISTS demo.login_attempts (
                                                   id BIGSERIAL PRIMARY KEY,
                                                   email VARCHAR(255) NOT NULL,
                                                   ip_address VARCHAR(45) NOT NULL,
                                                   attempt_time TIMESTAMP NOT NULL,
                                                   successful BOOLEAN NOT NULL
);

CREATE TABLE payment_history (
                                 id BIGSERIAL PRIMARY KEY,
                                 user_id BIGINT NOT NULL REFERENCES users(id),
                                 transaction_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 transaction_type VARCHAR(50) NOT NULL,
                                 points_amount INTEGER NOT NULL,
                                 description TEXT NOT NULL,
                                 balance_after INTEGER NOT NULL,
                                 related_entity_id BIGINT,  -- ID powiązanego kursu lub zadania
                                 related_entity_type VARCHAR(50) -- 'COURSE', 'TASK' itp.
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_points ON users(points);

CREATE INDEX IF NOT EXISTS idx_reviews_target ON reviews(target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_reviews_created_at ON reviews(created_at);
CREATE INDEX IF NOT EXISTS idx_reviews_user ON reviews(user_id);

CREATE INDEX IF NOT EXISTS courses_name_idx ON courses(name);
CREATE INDEX IF NOT EXISTS courses_tags_idx ON courses USING gin(tags);
CREATE INDEX IF NOT EXISTS courses_review_idx ON courses(review);
CREATE INDEX IF NOT EXISTS courses_review_number_idx ON courses(review_number);
CREATE INDEX IF NOT EXISTS idx_courses_user ON courses(user_id);
CREATE INDEX IF NOT EXISTS idx_courses_dates ON courses(created_at, updated_at);
CREATE INDEX IF NOT EXISTS idx_courses_price ON courses(price);

CREATE INDEX IF NOT EXISTS chapters_course_id_idx ON chapters(course_id);
CREATE INDEX IF NOT EXISTS subchapters_chapter_id_idx ON subchapters(chapter_id);

CREATE INDEX IF NOT EXISTS tasks_student_id_idx ON demo.tasks(student_id);
CREATE INDEX IF NOT EXISTS tasks_assigned_teacher_id_idx ON demo.tasks(assigned_teacher_id);
CREATE INDEX IF NOT EXISTS tasks_status_idx ON demo.tasks(status);
CREATE INDEX IF NOT EXISTS tasks_is_public_idx ON demo.tasks(is_public);
CREATE INDEX IF NOT EXISTS tasks_is_active_idx ON demo.tasks(is_active);

CREATE INDEX IF NOT EXISTS idx_login_attempts_email ON demo.login_attempts(email, attempt_time);
CREATE INDEX IF NOT EXISTS idx_login_attempts_ip ON demo.login_attempts(ip_address, attempt_time);

CREATE INDEX IF NOT EXISTS idx_payment_history_user ON payment_history(user_id);

CREATE INDEX IF NOT EXISTS idx_teacher_profiles_review ON teacher_profiles(review DESC, review_number DESC);
