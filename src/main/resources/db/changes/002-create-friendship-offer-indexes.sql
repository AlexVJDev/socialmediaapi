--liquibase formatted sql

--changeset student:002
CREATE INDEX idx_friendship_offer_incoming ON friendship_offer (to_user_id, status, created_at DESC);

CREATE INDEX idx_friendship_offer_outgoing ON friendship_offer (from_user_id, status, created_at DESC);

--rollback DROP INDEX IF EXISTS idx_friendship_offer_outgoing;
--rollback DROP INDEX IF EXISTS idx_friendship_offer_incoming;
