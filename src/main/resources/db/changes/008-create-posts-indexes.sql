--liquibase formatted sql

--changeset student:008
CREATE INDEX idx_posts_author_created ON posts (author_id, created_at DESC, id DESC);

--rollback DROP INDEX IF EXISTS idx_posts_author_created;
