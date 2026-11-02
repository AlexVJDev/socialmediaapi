--liquibase formatted sql

--changeset student:008
CREATE INDEX idx_posts_published_author_created
    ON posts (author_id, created_at DESC, id DESC)
    WHERE status = 'PUBLISHED';

--rollback DROP INDEX IF EXISTS idx_posts_published_author_created;
