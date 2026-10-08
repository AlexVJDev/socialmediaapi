--liquibase formatted sql

--changeset student:004
CREATE INDEX idx_friendships_first_user ON friendships (first_user_id);

CREATE INDEX idx_friendships_second_user ON friendships (second_user_id);

--rollback DROP INDEX IF EXISTS idx_friendships_second_user;
--rollback DROP INDEX IF EXISTS idx_friendships_first_user;
