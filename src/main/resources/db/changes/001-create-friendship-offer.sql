--liquibase formatted sql

--changeset student:001
CREATE TABLE friendship_offer
(
    id              UUID            PRIMARY KEY,
    from_user_id    UUID            NOT NULL,
    to_user_id      UUID            NOT NULL,
    status          VARCHAR(16)     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL,
    CONSTRAINT chk_friendship_offer_different_users CHECK (from_user_id <> to_user_id),
    CONSTRAINT chk_friendship_offer_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT uq_friendship_offer_users UNIQUE (from_user_id, to_user_id)
);

--rollback DROP TABLE IF EXISTS friendship_offer;
