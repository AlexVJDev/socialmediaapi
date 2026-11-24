--liquibase formatted sql

--changeset student:009
CREATE UNIQUE INDEX uq_friendship_offer_pair
    ON friendship_offer (LEAST(from_user_id, to_user_id), GREATEST(from_user_id, to_user_id));

--rollback DROP INDEX uq_friendship_offer_pair;
