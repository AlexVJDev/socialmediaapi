--liquibase formatted sql

--changeset student:011
-- Страницы заявок текущего пользователя (GET /api/v1/friendship-offer?direction=...) и друзей (GET /api/v1/friendships):
-- фильтр по пользователю, порядок created_at DESC, id DESC и курсор (created_at, id) < (?, ?) читаются по индексу
-- без сортировки. Индекс страниц всех заявок больше не нужен: запроса по всей таблице нет.
DROP INDEX idx_friendship_offer_created;

DROP INDEX idx_friendship_offer_incoming;
CREATE INDEX idx_friendship_offer_incoming ON friendship_offer (to_user_id, status, created_at DESC, id DESC);

DROP INDEX idx_friendship_offer_outgoing;
CREATE INDEX idx_friendship_offer_outgoing ON friendship_offer (from_user_id, status, created_at DESC, id DESC);

DROP INDEX idx_friendships_first_user;
CREATE INDEX idx_friendships_first_user_page ON friendships (first_user_id, created_at DESC, id DESC);

DROP INDEX idx_friendships_second_user;
CREATE INDEX idx_friendships_second_user_page ON friendships (second_user_id, created_at DESC, id DESC);

--rollback DROP INDEX idx_friendships_second_user_page;
--rollback CREATE INDEX idx_friendships_second_user ON friendships (second_user_id);
--rollback DROP INDEX idx_friendships_first_user_page;
--rollback CREATE INDEX idx_friendships_first_user ON friendships (first_user_id);
--rollback DROP INDEX idx_friendship_offer_outgoing;
--rollback CREATE INDEX idx_friendship_offer_outgoing ON friendship_offer (from_user_id, status, created_at DESC);
--rollback DROP INDEX idx_friendship_offer_incoming;
--rollback CREATE INDEX idx_friendship_offer_incoming ON friendship_offer (to_user_id, status, created_at DESC);
--rollback CREATE INDEX idx_friendship_offer_created ON friendship_offer (created_at DESC, id DESC);
