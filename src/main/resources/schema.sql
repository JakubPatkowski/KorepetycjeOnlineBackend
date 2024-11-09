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
DROP TABLE IF EXISTS users, user_profiles, refresh_tokens, verification_tokens, courses, chapters, subchapters, content_items, files, points_offers, purchased_courses CASCADE;

-- Tworzenie tabeli Users
CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(60)     NOT NULL,
    points        INTEGER   DEFAULT 0,
    verified      BOOLEAN   DEFAULT FALSE,
    blocked       BOOLEAN   DEFAULT FALSE,
    mfa           BOOLEAN   DEFAULT FALSE,
    role          VARCHAR(255)
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
    active BOOLEAN NOT NULL
);

CREATE TABLE purchased_courses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    course_id BIGINT REFERENCES courses(id),
    purchase_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    points_spent INTEGER NOT NULL,
    UNIQUE(user_id, course_id)
);
