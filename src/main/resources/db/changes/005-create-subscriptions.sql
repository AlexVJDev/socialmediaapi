--liquibase formatted sql

--changeset student:005
CREATE TABLE subscriptions
(
    id              UUID            PRIMARY KEY,
    follower_id     UUID            NOT NULL,
    followed_id     UUID            NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL,
    CONSTRAINT chk_subscriptions_different_users CHECK (follower_id <> followed_id),
    CONSTRAINT uq_subscriptions_users UNIQUE (follower_id, followed_id)
);

--rollback DROP TABLE IF EXISTS subscriptions;
