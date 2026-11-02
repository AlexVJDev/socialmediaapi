--liquibase formatted sql

--changeset student:007
CREATE TABLE posts
(
    id              UUID            PRIMARY KEY,
    author_id       UUID            NOT NULL,
    title           VARCHAR(255)    NOT NULL,
    content         TEXT            NOT NULL,
    status          VARCHAR(16)     NOT NULL DEFAULT 'PUBLISHED',
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL,
    CONSTRAINT chk_posts_status CHECK (status IN ('PUBLISHED', 'DELETED'))
);

--rollback DROP TABLE IF EXISTS posts;
