--liquibase formatted sql

--changeset student:003
CREATE TABLE friendships
(
    id                      UUID            PRIMARY KEY,
    first_user_id           UUID            NOT NULL,
    second_user_id          UUID            NOT NULL,
    friendship_offer_id     UUID            NOT NULL,
    status                  VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    created_at              TIMESTAMPTZ     NOT NULL,
    CONSTRAINT chk_friendships_canonical_order CHECK (first_user_id < second_user_id),
    CONSTRAINT uq_friendships_users UNIQUE (first_user_id, second_user_id),
    CONSTRAINT fk_friendships_friendship_offer FOREIGN KEY (friendship_offer_id) REFERENCES friendship_offer (id),
    CONSTRAINT chk_friendships_status CHECK (status IN ('ACTIVE', 'DELETED'))
);

--rollback DROP TABLE IF EXISTS friendships;
