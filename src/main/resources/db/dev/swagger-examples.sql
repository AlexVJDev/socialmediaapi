--liquibase formatted sql

-- Данные для Try it out в Swagger UI: записи с id из примеров контракта docs/api, чтобы запросы
-- с подставленными примерами находили данные, а не возвращали 404.
-- Только для локального запуска: changeset с контекстом dev, применяется при LIQUIBASE_CONTEXTS=dev
-- (по умолчанию контекст default — в тестах и на рабочих окружениях не выполняется).
-- runAlways: при каждом запуске приложения данные пользователей-примеров удаляются и создаются заново,
-- так что изменения через Try it out (accept, reject, delete, новые посты) сбрасываются перезапуском.
--
-- Пользователи (только UUID, таблицы пользователей нет):
--   A 0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01  отправитель заявок
--   B 5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22  получатель заявок
--   C 7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66  друг A, отправил заявку B
--   D a3c8e5f1-6d2b-4f7e-9a10-8b4c2d6e7f88  получатель заявки A
--   P 22222222-2222-2222-2222-222222222222  автор поста
--
-- Заявки (friendship_offer):
--   9c4e1b77-…3e44  A → B  PENDING   get / accept / reject (actorId = B); create A → B даёт 200 ALREADY_PENDING
--   2f6b9d13-…2a55  C → B  PENDING   входящие B
--   e8a4c2d6-…4b77  A → D  PENDING   исходящие A
--   c1d2e3f4-…4e00  A → C  ACCEPTED  основание дружбы A и C (в контракте не упоминается)
-- Дружба (friendships): c1d2e3f4-…4e02  A и C.
-- Подписки (subscriptions), как в бизнес-процессе: отправитель → получатель на каждую заявку,
-- для ACCEPTED — взаимные:
--   d4e5f6a7-…6f03  A → B   (подписки A; подписчики B; DELETE /subscribes/{B} от A)
--   d4e5f6a7-…6f02  C → B   (подписчики B)
--   d4e5f6a7-…6f04  A → D   (подписки A)
--   d4e5f6a7-…6f05  A → C,  d4e5f6a7-…6f06  C → A  (дружба A и C)
--   d4e5f6a7-…6f07  A → P   (лента A)
-- Посты (posts):
--   7d1f3c2a-…6a7b  автор P  get / put / delete (X-User-Id = P), посты P, лента A
--   7d1f3c2a-…6a7c  автор B  лента A

--changeset socialmediaapi:swagger-examples context:dev runAlways:true runOnChange:true
DELETE FROM friendships
WHERE first_user_id IN ('0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01', '5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22',
                        '7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66', 'a3c8e5f1-6d2b-4f7e-9a10-8b4c2d6e7f88',
                        '22222222-2222-2222-2222-222222222222')
   OR second_user_id IN ('0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01', '5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22',
                         '7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66', 'a3c8e5f1-6d2b-4f7e-9a10-8b4c2d6e7f88',
                         '22222222-2222-2222-2222-222222222222');

DELETE FROM friendship_offer
WHERE from_user_id IN ('0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01', '5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22',
                       '7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66', 'a3c8e5f1-6d2b-4f7e-9a10-8b4c2d6e7f88',
                       '22222222-2222-2222-2222-222222222222')
   OR to_user_id IN ('0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01', '5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22',
                     '7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66', 'a3c8e5f1-6d2b-4f7e-9a10-8b4c2d6e7f88',
                     '22222222-2222-2222-2222-222222222222');

DELETE FROM subscriptions
WHERE follower_id IN ('0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01', '5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22',
                      '7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66', 'a3c8e5f1-6d2b-4f7e-9a10-8b4c2d6e7f88',
                      '22222222-2222-2222-2222-222222222222')
   OR followed_id IN ('0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01', '5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22',
                      '7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66', 'a3c8e5f1-6d2b-4f7e-9a10-8b4c2d6e7f88',
                      '22222222-2222-2222-2222-222222222222');

DELETE FROM posts
WHERE author_id IN ('0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01', '5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22',
                    '7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66', 'a3c8e5f1-6d2b-4f7e-9a10-8b4c2d6e7f88',
                    '22222222-2222-2222-2222-222222222222');

INSERT INTO friendship_offer (id, from_user_id, to_user_id, status, created_at, updated_at)
VALUES ('c1d2e3f4-5a6b-4c7d-8e9f-0a1b2c3d4e00', '0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01',
        '7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66', 'ACCEPTED', '2026-09-20T09:00:00Z', '2026-09-20T09:05:00Z'),
       ('9c4e1b77-3f2a-4d6c-8b0e-1a5f7d2c3e44', '0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01',
        '5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22', 'PENDING', '2026-09-22T10:15:00Z', '2026-09-22T10:15:00Z'),
       ('2f6b9d13-8e4a-4b1c-a7d5-3c9e0f1b2a55', '7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66',
        '5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22', 'PENDING', '2026-09-22T10:25:00Z', '2026-09-22T10:25:00Z'),
       ('e8a4c2d6-1f3b-4e9d-b5c7-6a2d8f0e4b77', '0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01',
        'a3c8e5f1-6d2b-4f7e-9a10-8b4c2d6e7f88', 'PENDING', '2026-09-22T10:30:00Z', '2026-09-22T10:30:00Z');

INSERT INTO friendships (id, first_user_id, second_user_id, friendship_offer_id, status, created_at)
VALUES ('c1d2e3f4-5a6b-4c7d-8e9f-0a1b2c3d4e02', '0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01',
        '7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66', 'c1d2e3f4-5a6b-4c7d-8e9f-0a1b2c3d4e00', 'ACTIVE',
        '2026-09-20T09:05:00Z');

INSERT INTO subscriptions (id, follower_id, followed_id, created_at)
VALUES ('d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f05', '0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01',
        '7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66', '2026-09-20T09:00:00Z'),
       ('d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f06', '7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66',
        '0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01', '2026-09-20T09:05:00Z'),
       ('d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f03', '0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01',
        '5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22', '2026-09-22T10:15:00Z'),
       ('d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f02', '7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66',
        '5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22', '2026-09-22T10:25:00Z'),
       ('d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f04', '0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01',
        'a3c8e5f1-6d2b-4f7e-9a10-8b4c2d6e7f88', '2026-09-22T10:30:00Z'),
       ('d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f07', '0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01',
        '22222222-2222-2222-2222-222222222222', '2026-10-01T08:00:00Z');

INSERT INTO posts (id, author_id, title, content, status, created_at, updated_at)
VALUES ('7d1f3c2a-5b6e-4f80-9a1b-2c3d4e5f6a7b', '22222222-2222-2222-2222-222222222222', 'Пост 1',
        'Текст поста', 'PUBLISHED', '2026-10-15T10:00:00.123456Z', '2026-10-15T10:00:00.123456Z'),
       ('7d1f3c2a-5b6e-4f80-9a1b-2c3d4e5f6a7c', '5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22', 'Пост Бориса',
        'Текст поста Бориса', 'PUBLISHED', '2026-10-14T09:00:00Z', '2026-10-14T09:00:00Z');
