--liquibase formatted sql

--changeset student:009
CREATE UNIQUE INDEX uq_friendship_offer_pair
    ON friendship_offer (LEAST(from_user_id, to_user_id), GREATEST(from_user_id, to_user_id));

ALTER TABLE friendship_offer DROP CONSTRAINT uq_friendship_offer_users;

--rollback ALTER TABLE friendship_offer ADD CONSTRAINT uq_friendship_offer_users UNIQUE (from_user_id, to_user_id);
--rollback DROP INDEX IF EXISTS uq_friendship_offer_pair;
