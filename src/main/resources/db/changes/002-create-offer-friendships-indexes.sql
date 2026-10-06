--liquibase formatted sql

--changeset student:002
CREATE INDEX IF NOT EXISTS idx_offer_friendships_incoming ON offer_friendships (to_user_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_offer_friendships_outgoing ON offer_friendships (from_user_id, status, created_at DESC);

--rollback DROP INDEX IF EXISTS idx_offer_friendships_outgoing;
--rollback DROP INDEX IF EXISTS idx_offer_friendships_incoming;
