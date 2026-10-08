--liquibase formatted sql

--changeset student:003
CREATE TABLE friendships
(
    id                      UUID            PRIMARY KEY,
    first_user_id           UUID            NOT NULL,
    second_user_id          UUID            NOT NULL,
    offer_friendship_id     UUID            NOT NULL,
    created_at              TIMESTAMPTZ     NOT NULL,
    CONSTRAINT chk_friendships_canonical_order CHECK (first_user_id < second_user_id),
    CONSTRAINT uq_friendships_users UNIQUE (first_user_id, second_user_id),
    CONSTRAINT fk_friendships_offer_friendship FOREIGN KEY (offer_friendship_id) REFERENCES offer_friendships (id)
);

--rollback DROP TABLE IF EXISTS friendships;
