--liquibase formatted sql

--changeset student:006

CREATE INDEX IF NOT EXISTS idx_subscriptions_follower ON subscriptions (follower_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_followed ON subscriptions (followed_id);

--rollback DROP INDEX IF EXISTS idx_subscriptions_followed;
--rollback DROP INDEX IF EXISTS idx_subscriptions_follower;
