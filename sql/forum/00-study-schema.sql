-- Forum schema bootstrap for the `study` database.
-- This script is safe to execute multiple times on a local environment.

CREATE DATABASE IF NOT EXISTS study DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE study;

CREATE TABLE IF NOT EXISTS user (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    username        VARCHAR(50)  NOT NULL,
    password        VARCHAR(100) NOT NULL,
    salt            VARCHAR(32)  NOT NULL,
    email           VARCHAR(100) NOT NULL,
    type            INT          NOT NULL DEFAULT 0,
    status          INT          NOT NULL DEFAULT 0,
    activation_code VARCHAR(100) NOT NULL,
    header_url      VARCHAR(255) NOT NULL,
    create_time     DATETIME     NOT NULL,
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_email (email),
    KEY idx_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS login_ticket (
    id         INT PRIMARY KEY AUTO_INCREMENT,
    user_id    INT          NOT NULL,
    ticket     VARCHAR(100) NOT NULL,
    status     INT          NOT NULL DEFAULT 0,
    expired    DATETIME     NOT NULL,
    UNIQUE KEY uk_login_ticket_ticket (ticket),
    KEY idx_login_ticket_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS discuss_post (
    id            INT PRIMARY KEY AUTO_INCREMENT,
    user_id       INT            NOT NULL,
    title         VARCHAR(255)   NOT NULL,
    content       TEXT           NOT NULL,
    media         TEXT           NULL,
    type          INT            NOT NULL DEFAULT 0,
    status        INT            NOT NULL DEFAULT 0,
    create_time   DATETIME       NOT NULL,
    comment_count INT            NOT NULL DEFAULT 0,
    score         DOUBLE         NOT NULL DEFAULT 0,
    KEY idx_discuss_post_user (user_id),
    KEY idx_discuss_post_status (status),
    KEY idx_discuss_post_type_time (type, create_time),
    KEY idx_discuss_post_score (score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS comment (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    user_id     INT          NOT NULL,
    entity_type INT          NOT NULL,
    entity_id   INT          NOT NULL,
    target_id   INT          NOT NULL DEFAULT 0,
    content     TEXT         NOT NULL,
    media       TEXT         NULL,
    status      INT          NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL,
    KEY idx_comment_entity (entity_type, entity_id),
    KEY idx_comment_user (user_id),
    KEY idx_comment_status (status),
    KEY idx_comment_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS message (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    from_id         INT          NOT NULL,
    to_id           INT          NOT NULL,
    conversation_id VARCHAR(255) NOT NULL,
    content         TEXT         NOT NULL,
    status          INT          NOT NULL DEFAULT 0,
    create_time     DATETIME     NOT NULL,
    KEY idx_message_conversation (conversation_id),
    KEY idx_message_from_to (from_id, to_id),
    KEY idx_message_to_status (to_id, status),
    KEY idx_message_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO user (
    id,
    username,
    password,
    salt,
    email,
    type,
    status,
    activation_code,
    header_url,
    create_time
)
VALUES (
    1,
    'system',
    'SYSTEM_PLACEHOLDER',
    '',
    'system@newke.local',
    1,
    1,
    'system',
    'https://static.newke.local/system.png',
    NOW()
)
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    email = VALUES(email),
    type = VALUES(type),
    status = VALUES(status);

ALTER TABLE user AUTO_INCREMENT = 100;
