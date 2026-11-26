--liquibase formatted sql

-- Данные для Try it out в Swagger UI: записи с id из примеров контракта docs/api, чтобы запросы
-- с подставленными примерами находили данные, а списки (GET) возвращали не меньше 10 элементов.
-- Только для локального запуска: changeset с контекстом dev, применяется при LIQUIBASE_CONTEXTS=dev
-- (по умолчанию контекст default — в тестах и на рабочих окружениях не выполняется).
-- runAlways: при каждом запуске приложения данные пользователей-примеров удаляются и создаются заново,
-- так что изменения через Try it out (create, accept, reject, subscribe, delete, новые посты) сбрасываются
-- перезапуском. Часть 1 — удаление, часть 2 — вставка; результат после повторного прогона тот же.
--
-- Авторизация в Swagger UI (Authorize → userIdHeader, заголовок X-User-Id):
--   A 0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01  Алиса — основной пользователь: от её имени работают ВСЕ запросы
--                                            с примерами из контракта
--   B 5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22  Борис — второй пользователь: получатель заявки A → B (9c4e…3e44)
--
-- Пользователи (только UUID, таблицы пользователей нет):
--   A, B — см. выше
--   C 7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66  друг A, отправил заявку B
--   D a3c8e5f1-6d2b-4f7e-9a10-8b4c2d6e7f88  получатель заявки A
--   E 3f8a2c6e-4b1d-4e7a-9c5f-1d2e3f4a5b60  без связей: пример toUserId для create (A → E даёт 201)
--   P 22222222-2222-2222-2222-222222222222  автор постов ленты A
--   G(n) 00000000-0000-4000-8000-0000000000nn, n = 1..92 — массовка для списков:
--     1..12  → A PENDING   входящие A; заявка G(1) — пример accept, G(2) — пример reject
--     21..30 → A ACCEPTED  друзья A
--     31..40 → A REJECTED
--     A → 41..52 PENDING   исходящие A
--     A → 61..70 ACCEPTED  друзья A
--     A → 71..80 REJECTED
--     81..92 подписаны на B (подписчики B)
--   Id заявок массовки: 0f000000-0000-4000-8000-0000000000nn (n — номер G), дружб — 0d…, подписок — 0c…, постов — 0e….
--
-- Что возвращают примеры контракта (X-User-Id = A):
--   GET /friendship-offer?direction=INCOMING|OUTGOING            12 | 14 заявок PENDING (по 10+ в ACCEPTED/REJECTED)
--   GET /friendship-offer/{9c4e…3e44}                             A → B PENDING
--   POST /friendship-offer/create {toUserId: E}                   201 (повтор — 200 ALREADY_PENDING)
--   POST /friendship-offer/accept {0f…01}, reject {0f…02}         200 (G(1) → A, G(2) → A)
--   GET /friendships?userId=A                                     21 друг
--   GET /subscribes?userId=A, /subscribes/followers?userId=B      46 | 14 подписок
--   POST /subscribes {userId: B}                                  200 (A уже подписана на B)
--   DELETE /subscribes/{B}                                        204
--   GET /users/{A}/posts                                          12 постов A
--   GET|PUT|DELETE /posts/{7d1f…6a7b}                             пост A
--   GET /feed                                                     13 постов (P и B)

--changeset socialmediaapi:swagger-examples context:dev runAlways:true runOnChange:true
-- Часть 1. Удаление данных пользователей-примеров (A, B, C, D, E, P и G(1..100)).
DELETE FROM friendships
WHERE first_user_id = ANY (ARRAY['0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01', '5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22',
                                 '7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66', 'a3c8e5f1-6d2b-4f7e-9a10-8b4c2d6e7f88',
                                 '3f8a2c6e-4b1d-4e7a-9c5f-1d2e3f4a5b60', '22222222-2222-2222-2222-222222222222']::uuid[]
                           || ARRAY(SELECT ('00000000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid
                                    FROM generate_series(1, 100) AS n))
   OR second_user_id = ANY (ARRAY['0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01', '5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22',
                                  '7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66', 'a3c8e5f1-6d2b-4f7e-9a10-8b4c2d6e7f88',
                                  '3f8a2c6e-4b1d-4e7a-9c5f-1d2e3f4a5b60', '22222222-2222-2222-2222-222222222222']::uuid[]
                            || ARRAY(SELECT ('00000000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid
                                     FROM generate_series(1, 100) AS n));

DELETE FROM friendship_offer
WHERE from_user_id = ANY (ARRAY['0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01', '5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22',
                                '7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66', 'a3c8e5f1-6d2b-4f7e-9a10-8b4c2d6e7f88',
                                '3f8a2c6e-4b1d-4e7a-9c5f-1d2e3f4a5b60', '22222222-2222-2222-2222-222222222222']::uuid[]
                          || ARRAY(SELECT ('00000000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid
                                   FROM generate_series(1, 100) AS n))
   OR to_user_id = ANY (ARRAY['0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01', '5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22',
                              '7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66', 'a3c8e5f1-6d2b-4f7e-9a10-8b4c2d6e7f88',
                              '3f8a2c6e-4b1d-4e7a-9c5f-1d2e3f4a5b60', '22222222-2222-2222-2222-222222222222']::uuid[]
                        || ARRAY(SELECT ('00000000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid
                                 FROM generate_series(1, 100) AS n));

DELETE FROM subscriptions
WHERE follower_id = ANY (ARRAY['0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01', '5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22',
                               '7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66', 'a3c8e5f1-6d2b-4f7e-9a10-8b4c2d6e7f88',
                               '3f8a2c6e-4b1d-4e7a-9c5f-1d2e3f4a5b60', '22222222-2222-2222-2222-222222222222']::uuid[]
                         || ARRAY(SELECT ('00000000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid
                                  FROM generate_series(1, 100) AS n))
   OR followed_id = ANY (ARRAY['0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01', '5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22',
                               '7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66', 'a3c8e5f1-6d2b-4f7e-9a10-8b4c2d6e7f88',
                               '3f8a2c6e-4b1d-4e7a-9c5f-1d2e3f4a5b60', '22222222-2222-2222-2222-222222222222']::uuid[]
                         || ARRAY(SELECT ('00000000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid
                                  FROM generate_series(1, 100) AS n));

DELETE FROM posts
WHERE author_id = ANY (ARRAY['0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01', '5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22',
                             '7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66', 'a3c8e5f1-6d2b-4f7e-9a10-8b4c2d6e7f88',
                             '3f8a2c6e-4b1d-4e7a-9c5f-1d2e3f4a5b60', '22222222-2222-2222-2222-222222222222']::uuid[]
                       || ARRAY(SELECT ('00000000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid
                                FROM generate_series(1, 100) AS n));

-- Часть 2. Вставка.
-- 2.1. Заявки из примеров контракта.
INSERT INTO friendship_offer (id, from_user_id, to_user_id, status, created_at, updated_at)
VALUES ('c1d2e3f4-5a6b-4c7d-8e9f-0a1b2c3d4e00', '0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01',
        '7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66', 'ACCEPTED', '2026-09-20T09:00:00Z', '2026-09-20T09:05:00Z'),
       ('9c4e1b77-3f2a-4d6c-8b0e-1a5f7d2c3e44', '0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01',
        '5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22', 'PENDING', '2026-09-22T10:15:00Z', '2026-09-22T10:15:00Z'),
       ('2f6b9d13-8e4a-4b1c-a7d5-3c9e0f1b2a55', '7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66',
        '5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22', 'PENDING', '2026-09-22T10:25:00Z', '2026-09-22T10:25:00Z'),
       ('e8a4c2d6-1f3b-4e9d-b5c7-6a2d8f0e4b77', '0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01',
        'a3c8e5f1-6d2b-4f7e-9a10-8b4c2d6e7f88', 'PENDING', '2026-09-22T10:30:00Z', '2026-09-22T10:30:00Z');

-- 2.2. Заявки массовки: входящие A (G(n) → A) и исходящие A (A → G(n)); у принятых updated_at на 5 минут позже.
INSERT INTO friendship_offer (id, from_user_id, to_user_id, status, created_at, updated_at)
SELECT ('0f000000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
       CASE WHEN n <= 40 THEN ('00000000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid
            ELSE '0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01'::uuid END,
       CASE WHEN n <= 40 THEN '0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01'::uuid
            ELSE ('00000000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid END,
       CASE WHEN n BETWEEN 1 AND 12 OR n BETWEEN 41 AND 52 THEN 'PENDING'
            WHEN n BETWEEN 21 AND 30 OR n BETWEEN 61 AND 70 THEN 'ACCEPTED'
            ELSE 'REJECTED' END,
       TIMESTAMPTZ '2026-10-01T09:00:00Z' + n * INTERVAL '1 hour',
       TIMESTAMPTZ '2026-10-01T09:00:00Z' + n * INTERVAL '1 hour'
           + CASE WHEN n BETWEEN 1 AND 12 OR n BETWEEN 41 AND 52 THEN INTERVAL '0' ELSE INTERVAL '5 minutes' END
FROM generate_series(1, 80) AS n
WHERE n BETWEEN 1 AND 12 OR n BETWEEN 21 AND 52 OR n BETWEEN 61 AND 80;

-- 2.3. Дружбы: A и C (пример контракта) и по каждой принятой заявке массовки; пара — в каноническом порядке.
INSERT INTO friendships (id, first_user_id, second_user_id, friendship_offer_id, status, created_at)
VALUES ('c1d2e3f4-5a6b-4c7d-8e9f-0a1b2c3d4e02', '0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01',
        '7d1f0a52-2b8e-4c9a-b3d6-4e8f1a2c5b66', 'c1d2e3f4-5a6b-4c7d-8e9f-0a1b2c3d4e00', 'ACTIVE',
        '2026-09-20T09:05:00Z');

INSERT INTO friendships (id, first_user_id, second_user_id, friendship_offer_id, status, created_at)
SELECT ('0d000000-0000-4000-8000-' || substr(o.id::text, 25))::uuid,
       LEAST(o.from_user_id, o.to_user_id), GREATEST(o.from_user_id, o.to_user_id), o.id, 'ACTIVE', o.updated_at
FROM friendship_offer o
WHERE o.id = ANY (ARRAY(SELECT ('0f000000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid
                        FROM generate_series(1, 80) AS n))
  AND o.status = 'ACCEPTED';

-- 2.4. Подписки из примеров контракта.
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

-- 2.5. Подписки массовки, как в бизнес-процессе: отправитель заявки → получатель (id 0c…01nn),
-- для принятых ещё и получатель → отправитель (id 0c…02nn); G(81..92) подписаны на B (id 0c…03nn).
INSERT INTO subscriptions (id, follower_id, followed_id, created_at)
SELECT ('0c000000-0000-4000-8000-0000000001' || lpad(substr(o.id::text, 35), 2, '0'))::uuid,
       o.from_user_id, o.to_user_id, o.created_at
FROM friendship_offer o
WHERE o.id = ANY (ARRAY(SELECT ('0f000000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid
                        FROM generate_series(1, 80) AS n));

INSERT INTO subscriptions (id, follower_id, followed_id, created_at)
SELECT ('0c000000-0000-4000-8000-0000000002' || lpad(substr(o.id::text, 35), 2, '0'))::uuid,
       o.to_user_id, o.from_user_id, o.updated_at
FROM friendship_offer o
WHERE o.id = ANY (ARRAY(SELECT ('0f000000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid
                        FROM generate_series(1, 80) AS n))
  AND o.status = 'ACCEPTED';

INSERT INTO subscriptions (id, follower_id, followed_id, created_at)
SELECT ('0c000000-0000-4000-8000-0000000003' || lpad(n::text, 2, '0'))::uuid,
       ('00000000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
       '5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22'::uuid,
       TIMESTAMPTZ '2026-10-10T09:00:00Z' + n * INTERVAL '1 hour'
FROM generate_series(81, 92) AS n;

-- 2.6. Посты: 7d1f…6a7b — пост A из примеров контракта (get / put / delete), ещё 11 постов A,
-- 12 постов P и пост B (лента A).
INSERT INTO posts (id, author_id, title, content, status, created_at, updated_at)
VALUES ('7d1f3c2a-5b6e-4f80-9a1b-2c3d4e5f6a7b', '0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01', 'Пост 1',
        'Текст поста', 'PUBLISHED', '2026-10-15T10:00:00.123456Z', '2026-10-15T10:00:00.123456Z'),
       ('7d1f3c2a-5b6e-4f80-9a1b-2c3d4e5f6a7c', '5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22', 'Пост Бориса',
        'Текст поста Бориса', 'PUBLISHED', '2026-10-14T09:00:00Z', '2026-10-14T09:00:00Z');

INSERT INTO posts (id, author_id, title, content, status, created_at, updated_at)
SELECT ('0e000000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
       CASE WHEN n <= 11 THEN '0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01'::uuid
            ELSE '22222222-2222-2222-2222-222222222222'::uuid END,
       CASE WHEN n <= 11 THEN 'Пост Алисы ' || (n + 1) ELSE 'Пост P ' || (n - 11) END,
       'Текст поста ' || n,
       'PUBLISHED',
       TIMESTAMPTZ '2026-10-01T12:00:00Z' + n * INTERVAL '3 hours',
       TIMESTAMPTZ '2026-10-01T12:00:00Z' + n * INTERVAL '3 hours'
FROM generate_series(1, 23) AS n;
