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
    picture          BYTEA        DEFAULT NULL
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
    banner_file_id BIGINT,
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
    "order" INTEGER NOT NULL,
    completed BOOLEAN DEFAULT FALSE
);

CREATE TABLE content_items (
    id BIGSERIAL PRIMARY KEY,
    subchapter_id BIGINT REFERENCES subchapters(id) ON DELETE CASCADE,
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



-- CREATE TABLE quiz_question (
--     id BIGSERIAL PRIMARY KEY,
--     content_item_id BIGINT REFERENCES content_items(id) ON DELETE CASCADE,
--     question TEXT NOT NULL,
--     order INTEGER NOT NULL,
--     single_answer BOOLEAN NOT NULL DEFAULT TRUE
-- );
--
-- CREATE TABLE answers(
--     id BIGSERIAL PRIMARY KEY,
--     quiz_question_id BIGINT REFERENCES  quiz_question(id) ON DELETE CASCADE,
--     answer TEXT N
-- );


--
-- -- Tworzenie tabeli Course_User
-- CREATE TABLE Course_User
-- (
--     course_id BIGINT NOT NULL REFERENCES Course (id),
--     user_id   BIGINT NOT NULL REFERENCES Users (id),
--     PRIMARY KEY (course_id, user_id)
-- );
--
-- -- Tworzenie tabeli Chapter
-- CREATE TABLE Chapter
-- (
--     id          BIGSERIAL PRIMARY KEY,
--     course_id   BIGINT       NOT NULL REFERENCES Course (id),
--     name        VARCHAR(255) NOT NULL,
--     description VARCHAR(10000) DEFAULT NULL
-- );
--
-- -- Tworzenie tabeli Files
-- CREATE TABLE Files
-- (
--     id         BIGSERIAL PRIMARY KEY,
--     chapter_id BIGINT       NOT NULL REFERENCES Chapter (id),
--     name       VARCHAR(255) NOT NULL,
--     file       BYTEA DEFAULT NULL
-- );
--
-- -- Tworzenie tabeli Login_session
-- CREATE TABLE Login_session
-- (
--     id         BIGSERIAL PRIMARY KEY,
--     user_id    BIGINT      NOT NULL REFERENCES Users (id),
--     token      VARCHAR(30) NOT NULL,
--     started_at DATE DEFAULT CURRENT_TIMESTAMP
-- );
--
-- -- Tworzenie tabeli Certificates
-- CREATE TABLE Certificates
-- (
--     id      BIGSERIAL PRIMARY KEY,
--     user_id BIGINT        NOT NULL REFERENCES Users (id),
--     name    VARCHAR(1000) NOT NULL,
--     file    BYTEA DEFAULT NULL
-- );
--
-- -- Tworzenie tabeli Review
-- CREATE TABLE Review
-- (
--     id          BIGSERIAL PRIMARY KEY,
--     reviewer_id BIGINT  NOT NULL REFERENCES Users (id),
--     rating      INTEGER NOT NULL,
--     content     TEXT    DEFAULT NULL,
--     type        INTEGER DEFAULT NULL
-- );
--
-- -- Tworzenie tabeli Conversation
-- CREATE TABLE Conversation
-- (
--     id   BIGSERIAL PRIMARY KEY,
--     name VARCHAR(255) NOT NULL
-- );
--
-- -- Tworzenie tabeli Conversation_User
-- CREATE TABLE Conversation_User
-- (
--     conversation_id BIGINT NOT NULL REFERENCES Conversation (id),
--     user_id         BIGINT NOT NULL REFERENCES Users (id),
--     PRIMARY KEY (conversation_id, user_id)
-- );
--
-- -- Tworzenie tabeli Message
-- CREATE TABLE Message
-- (
--     id              BIGSERIAL PRIMARY KEY,
--     conversation_id BIGINT        NOT NULL REFERENCES Conversation (id),
--     sender_id       BIGINT        NOT NULL REFERENCES Users (id),
--     content         VARCHAR(1000) NOT NULL,
--     send_at         DATE  DEFAULT CURRENT_TIMESTAMP,
--     file            BYTEA DEFAULT NULL
-- );
--
-- -- Tworzenie tabeli Report
-- CREATE TABLE Report
-- (
--     id              BIGSERIAL PRIMARY KEY,
--     reporter_id     BIGINT NOT NULL REFERENCES Users (id),
--     conversation_id BIGINT NOT NULL REFERENCES Conversation (id),
--     content         VARCHAR(5000) DEFAULT NULL,
--     subject_id      BIGINT NOT NULL,
--     type            INTEGER       DEFAULT NULL
-- );
--
-- -- Tworzenie tabeli Badges
-- CREATE TABLE Badges
-- (
--     id         BIGSERIAL PRIMARY KEY,
--     user_id    BIGINT NOT NULL REFERENCES Users (id),
--     visibility BOOLEAN DEFAULT TRUE
-- );
--
-- -- Tworzenie tabeli User_Badges
-- CREATE TABLE User_Badges
-- (
--     user_id   BIGINT NOT NULL REFERENCES Users (id),
--     badges_id BIGINT NOT NULL REFERENCES Badges (id),
--     PRIMARY KEY (user_id, badges_id)
-- );
--
-- -- Tworzenie tabeli Private_lesson
-- CREATE TABLE Private_lesson
-- (
--     id          BIGSERIAL PRIMARY KEY,
--     owner_id    BIGINT       NOT NULL REFERENCES Users (id),
--     name        VARCHAR(255) NOT NULL,
--     description VARCHAR(5000) DEFAULT NULL,
--     started_at  DATE          DEFAULT CURRENT_TIMESTAMP,
--     ended_at    DATE          DEFAULT NULL,
--     price       INTEGER      NOT NULL,
--     visibility  BOOLEAN       DEFAULT TRUE
-- );
--
-- -- Tworzenie tabeli User_Private_lesson
-- CREATE TABLE User_Private_lesson
-- (
--     user_id           BIGINT NOT NULL REFERENCES Users (id),
--     private_lesson_id BIGINT NOT NULL REFERENCES Private_lesson (id),
--     PRIMARY KEY (user_id, private_lesson_id)
-- );
--


-- CREATE TYPE permission_enum AS ENUM ('buyPoints', 'createCourse', 'createLesson');

--
-- -- Tworzenie tabeli Token_price
-- CREATE TABLE Token_price
-- (
--     id               BIGSERIAL PRIMARY KEY,
--     price_amount     NUMERIC(19, 2) NOT NULL,
--     token_amount     INTEGER        NOT NULL,
--     transaction_type VARCHAR(255) DEFAULT NULL
-- );
--
-- -- Tworzenie tabeli Transaction_history
-- CREATE TABLE Transaction_history
-- (
--     id        BIGSERIAL PRIMARY KEY,
--     user_id   BIGINT NOT NULL REFERENCES Users (id),
--     target_id BIGINT  DEFAULT NULL,
--     type      INTEGER DEFAULT NULL,
--     date      DATE    DEFAULT CURRENT_TIMESTAMP
-- );
--
-- -- Tworzenie tabeli Commision
-- CREATE TABLE Commision
-- (
--     id                     BIGSERIAL PRIMARY KEY,
--     taker_id               BIGINT  NOT NULL REFERENCES Users (id),
--     time_before_expiration TIME          DEFAULT NULL,
--     name                   VARCHAR(255)  DEFAULT NULL,
--     description            VARCHAR(5000) DEFAULT NULL,
--     file                   BYTEA         DEFAULT NULL,
--     price                  INTEGER NOT NULL,
--     visibility             BOOLEAN       DEFAULT TRUE
-- );
--
-- -- Tworzenie tabeli User_Commision
-- CREATE TABLE User_Commision
-- (
--     user_id      BIGINT NOT NULL REFERENCES Users (id),
--     commision_id BIGINT NOT NULL REFERENCES Commision (id),
--     PRIMARY KEY (user_id, commision_id)
-- );
--
--
-- -- Tworzenie tabeli lesson_commision
-- CREATE TABLE lesson_commision
-- (
--     commision_id      BIGINT NOT NULL REFERENCES Commision (id),
--     private_lesson_id BIGINT NOT NULL REFERENCES Private_lesson (id),
--     start_time        DATE DEFAULT CURRENT_TIMESTAMP,
--     end_time          DATE DEFAULT NULL,
--     PRIMARY KEY (commision_id, private_lesson_id)
-- );
--
-- -- Tworzenie tabeli exercise_commision
-- CREATE TABLE exercise_commision
-- (
--     commision_id     BIGINT NOT NULL REFERENCES Commision (id),
--     time_to_complete TIME             DEFAULT NULL,
--     response_text    VARCHAR(5000000) DEFAULT NULL,
--     response_file    BYTEA            DEFAULT NULL,
--     PRIMARY KEY (commision_id)
-- );

--

