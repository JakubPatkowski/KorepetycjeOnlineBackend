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
DROP TABLE IF EXISTS users, user_profiles, refresh_tokens, verification_tokens, courses, chapters, subchapters, content_items, files CASCADE;

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

CREATE TABLE files (
                       id BIGSERIAL PRIMARY KEY,
                       file_name VARCHAR(255) NOT NULL,
                       mime_type VARCHAR(127) NOT NULL,
                       file_size BIGINT NOT NULL,
                       file_content BYTEA NOT NULL,
                       uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
    banner_file_id BIGINT REFERENCES files(id),
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
    text_content TEXT,
    font_size VARCHAR(20),  -- 'small', 'medium', 'large'
    font_weight VARCHAR(20), -- 'normal', 'bold'
    italics BOOLEAN,
    emphasis BOOLEAN,
    -- File specific fields
    file_id BIGINT REFERENCES files(id),-- For video and image content
        -- Quiz specific fields (using JSONB for flexible structure)
    quiz_data JSONB, -- Stores quiz questions and answers in this format:
    /*
    {
        "questions": [
            {
                "id": 1,
                "question": "What is...?",
                "order": 1,
                "singleAnswer": true,
                "answers": [
                    {
                        "id": 1,
                        "answer": "Option A",
                        "order": 1,
                        "isCorrect": true
                    },
                    {
                        "id": 2,
                        "answer": "Option B",
                        "order": 2,
                        "isCorrect": false
                    }
                ]
            }
        ]
    }
    */

    CHECK (
        (type = 'text' AND text_content IS NOT NULL) OR
        (type IN ('video', 'image') AND file_id IS NOT NULL) OR
        (type = 'quiz' AND quiz_data IS NOT NULL)
        )

);


