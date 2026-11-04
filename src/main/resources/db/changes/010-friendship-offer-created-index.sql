--liquibase formatted sql

--changeset student:010
-- Страницы всех заявок (GET /api/v1/friendship-offer): порядок created_at DESC, id DESC и курсор
-- (created_at, id) < (?, ?) читаются по индексу без сортировки таблицы.
CREATE INDEX idx_friendship_offer_created ON friendship_offer (created_at DESC, id DESC);

--rollback DROP INDEX IF EXISTS idx_friendship_offer_created;
