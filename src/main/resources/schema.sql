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
DROP TABLE IF EXISTS Course_User, Chapter, Course, Files, Login_session, Certificates, Review, Conversation_User, Conversation, Badges, User_Badges, Private_lesson, User_Private_lesson, Token_price, Transaction_history, Report, Message, Commision, User_Commision, permission_role, permission, lesson_commision, exercise_commision CASCADE;
DROP TABLE IF EXISTS users, user_profile, refresh_token CASCADE;


DROP TYPE IF EXISTS role_enum;

CREATE TYPE role_enum AS ENUM ('USER', 'TEACHER', 'ADMIN');

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
CREATE TABLE user_profile
(
    id          BIGSERIAL PRIMARY KEY,
    name_and_surname     VARCHAR(255) NOT NULL,
    user_id     BIGSERIAL    NOT NULL REFERENCES users (id),
    description VARCHAR(500) DEFAULT NULL,
    created_at  DATE         DEFAULT CURRENT_TIMESTAMP,
    picture     BYTEA        DEFAULT NULL
);

CREATE TABLE refresh_token
(
    id         BIGSERIAL PRIMARY KEY,
    ref_token  VARCHAR(100) NOT NULL,
    user_id    BIGSERIAL    NOT NULL REFERENCES users (id),
    ip         VARCHAR(200) NOT NULL,
    expiration TIMESTAMP    NOT NULL,
    created_at TIMESTAMP    NOT NULL
);

-- -- Tworzenie tabeli Course
-- CREATE TABLE Course
-- (
--     id          BIGSERIAL PRIMARY KEY,
--     owner_id    BIGINT        NOT NULL REFERENCES Users (id),
--     description VARCHAR(5000) NOT NULL,
--     price       INTEGER       NOT NULL,
--     banner      BYTEA   DEFAULT NULL,
--     visibility  BOOLEAN DEFAULT TRUE
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

