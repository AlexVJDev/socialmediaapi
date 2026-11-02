--liquibase formatted sql

-- Тестовые данные для всех таблиц: ручные проверки и EXPLAIN ленты (задача 41215-2).
-- В db.changelog-master.yaml НЕ подключается: выполняется вручную на локальной базе, НЕ на рабочей.
--   docker compose exec -T postgres psql -U <DB_USERNAME> -d <DB> < src/main/resources/db/changes/0091-sql-examples.sql
-- Запускать на пустых таблицах (после rollback ниже): повторный запуск упадёт на фиксированных UUID.
-- Время — около 30–90 секунд, почти всё уходит на posts.
--
-- Что создаётся:
--   пользователи  — 5 000: 5 фиксированных (таблица ниже) + 4 995 случайных UUID;
--   posts         — 2 000 000: по 400 у каждого пользователя (и у фиксированных), за последний год,
--                   ~5 % DELETED;
--   friendship_offer — 4 фиксированные заявки + ~20 000 случайных между случайными пользователями
--                   (~50 % ACCEPTED, ~30 % PENDING, ~20 % REJECTED, одна заявка на пару);
--   friendships   — по одной на каждую ACCEPTED-заявку, пара в каноническом порядке;
--   subscriptions — как в бизнес-процессе: отправитель → получатель для каждой заявки,
--                   получатель → отправитель для ACCEPTED; плюс подписки без заявок (ниже)
--                   и ~50 000 случайных.
--
-- Фиксированные пользователи:
--   11111111-1111-1111-1111-111111111111  Алиса — подписана на Бориса, Веру и 200 случайных авторов
--   22222222-2222-2222-2222-222222222222  Борис
--   33333333-3333-3333-3333-333333333333  Вера
--   44444444-4444-4444-4444-444444444444  Глеб
--   55555555-5555-5555-5555-555555555555  Дина — «читатель ленты»: подписана на Алису, Бориса, Веру
--                                         и 1 000 случайных авторов, заявок нет
--
-- Фиксированные заявки:
--   aaaaaaaa-aaaa-aaaa-aaaa-000000000012  Алиса → Борис  ACCEPTED (друзья, взаимные подписки)
--   aaaaaaaa-aaaa-aaaa-aaaa-000000000013  Алиса → Вера   PENDING  (Алиса подписана на Веру)
--   aaaaaaaa-aaaa-aaaa-aaaa-000000000041  Глеб  → Алиса  REJECTED (Глеб подписан на Алису)
--   aaaaaaaa-aaaa-aaaa-aaaa-000000000023  Борис → Вера   ACCEPTED (друзья, взаимные подписки)
--
-- Statistics обновляются ANALYZE в конце. Для Index Only Scan без Heap Fetches после загрузки
-- выполнить вручную (VACUUM нельзя внутри транзакции changeset):
--   VACUUM ANALYZE posts, subscriptions;
--
-- Удаление — rollback ниже: TRUNCATE удаляет ВСЕ строки четырёх таблиц, в том числе созданные
-- приложением. При ручном запуске — выполнить строку rollback без «--rollback».

--changeset student:0091

-- =====================================================================================
-- 1. Пользователи: 5 фиксированных (n 1..5) и 4 995 случайных (n 6..5000).
--    Номер n нужен, чтобы выбирать случайных пользователей и делить между ними посты.
-- =====================================================================================
CREATE TEMP TABLE bench_users
(
    n   INT     PRIMARY KEY,
    id  UUID    NOT NULL UNIQUE
);

INSERT INTO bench_users (n, id)
    VALUES
        (1, '11111111-1111-1111-1111-111111111111'),
        (2, '22222222-2222-2222-2222-222222222222'),
        (3, '33333333-3333-3333-3333-333333333333'),
        (4, '44444444-4444-4444-4444-444444444444'),
        (5, '55555555-5555-5555-5555-555555555555');

INSERT INTO bench_users (n, id)
    SELECT  n,
            gen_random_uuid()
        FROM generate_series(6, 5000) n;

-- =====================================================================================
-- 2. Фиксированные заявки в друзья.
-- =====================================================================================
INSERT INTO friendship_offer (id, from_user_id, to_user_id, status, created_at, updated_at)
    VALUES
        ('aaaaaaaa-aaaa-aaaa-aaaa-000000000012',            -- Алиса → Борис
         '11111111-1111-1111-1111-111111111111',
         '22222222-2222-2222-2222-222222222222',
         'ACCEPTED',
         now() - INTERVAL '30 days',
         now() - INTERVAL '29 days'),

        ('aaaaaaaa-aaaa-aaaa-aaaa-000000000013',            -- Алиса → Вера
         '11111111-1111-1111-1111-111111111111',
         '33333333-3333-3333-3333-333333333333',
         'PENDING',
         now() - INTERVAL '10 days',
         now() - INTERVAL '10 days'),

        ('aaaaaaaa-aaaa-aaaa-aaaa-000000000041',            -- Глеб → Алиса
         '44444444-4444-4444-4444-444444444444',
         '11111111-1111-1111-1111-111111111111',
         'REJECTED',
         now() - INTERVAL '20 days',
         now() - INTERVAL '19 days'),

        ('aaaaaaaa-aaaa-aaaa-aaaa-000000000023',            -- Борис → Вера
         '22222222-2222-2222-2222-222222222222',
         '33333333-3333-3333-3333-333333333333',
         'ACCEPTED',
         now() - INTERVAL '15 days',
         now() - INTERVAL '14 days');

-- =====================================================================================
-- 3. Случайные заявки — только между случайными пользователями (n >= 6),
--    одна на пару в любом направлении (DISTINCT ON).
--    «+ g * 0» связывает подзапрос со строкой, иначе random() вычислится один раз на весь запрос.
-- =====================================================================================
INSERT INTO friendship_offer (id, from_user_id, to_user_id, status, created_at, updated_at)
    SELECT DISTINCT ON (LEAST(f.id, t.id), GREATEST(f.id, t.id))
            gen_random_uuid(),
            f.id,
            t.id,
            CASE
                WHEN x.s < 0.5 THEN 'ACCEPTED'
                WHEN x.s < 0.8 THEN 'PENDING'
                ELSE 'REJECTED'
            END,
            x.ts,
            CASE
                WHEN x.s >= 0.5 AND x.s < 0.8 THEN x.ts
                ELSE LEAST(x.ts + INTERVAL '1 hour', now())
            END
        FROM generate_series(1, 20000) g
            CROSS JOIN LATERAL (
                SELECT  6 + floor(random() * 4995)::INT + g * 0                   AS from_n,
                        6 + floor(random() * 4995)::INT + g * 0                   AS to_n,
                        random() + g * 0                                          AS s,
                        now() - make_interval(secs => random() * 31536000 + g * 0) AS ts
            ) x
            JOIN bench_users f ON f.n = x.from_n
            JOIN bench_users t ON t.n = x.to_n
        WHERE f.id <> t.id;

-- =====================================================================================
-- 4. Дружбы — по одной на каждую ACCEPTED-заявку, пара в каноническом порядке.
--    Строятся из всех заявок таблицы: скрипт запускается на пустых таблицах.
-- =====================================================================================
INSERT INTO friendships (id, first_user_id, second_user_id, friendship_offer_id, status, created_at)
    SELECT  gen_random_uuid(),
            LEAST(from_user_id, to_user_id),
            GREATEST(from_user_id, to_user_id),
            id,
            'ACTIVE',
            updated_at
        FROM friendship_offer
        WHERE status = 'ACCEPTED';

-- =====================================================================================
-- 5. Подписки бизнес-процесса:
--    отправитель → получатель для каждой заявки (create),
--    получатель → отправитель для ACCEPTED (accept).
-- =====================================================================================
INSERT INTO subscriptions (id, follower_id, followed_id, created_at)
    SELECT  gen_random_uuid(),
            from_user_id,
            to_user_id,
            created_at
        FROM friendship_offer

    UNION ALL

    SELECT  gen_random_uuid(),
            to_user_id,
            from_user_id,
            updated_at
        FROM friendship_offer
        WHERE status = 'ACCEPTED';

-- =====================================================================================
-- 6. Дина: Алиса, Борис, Вера и 1 000 случайных авторов (n 6..1005).
-- =====================================================================================
INSERT INTO subscriptions (id, follower_id, followed_id, created_at)
    SELECT  gen_random_uuid(),
            '55555555-5555-5555-5555-555555555555',
            id,
            now() - INTERVAL '1 day'
        FROM bench_users
        WHERE n BETWEEN 1 AND 3
            OR n BETWEEN 6 AND 1005;

-- =====================================================================================
-- 7. Алиса: ещё 200 случайных авторов (n 1006..1205).
-- =====================================================================================
INSERT INTO subscriptions (id, follower_id, followed_id, created_at)
    SELECT  gen_random_uuid(),
            '11111111-1111-1111-1111-111111111111',
            id,
            now() - INTERVAL '1 day'
        FROM bench_users
        WHERE n BETWEEN 1006 AND 1205;

-- =====================================================================================
-- 8. Случайные подписки без заявок между случайными пользователями; дубликаты пропускаются.
-- =====================================================================================
INSERT INTO subscriptions (id, follower_id, followed_id, created_at)
    SELECT  gen_random_uuid(),
            f.id,
            t.id,
            x.ts
        FROM generate_series(1, 50000) g
            CROSS JOIN LATERAL (
                SELECT  6 + floor(random() * 4995)::INT + g * 0                   AS follower_n,
                        6 + floor(random() * 4995)::INT + g * 0                   AS followed_n,
                        now() - make_interval(secs => random() * 31536000 + g * 0) AS ts
            ) x
            JOIN bench_users f ON f.n = x.follower_n
            JOIN bench_users t ON t.n = x.followed_n
        WHERE f.id <> t.id
    ON CONFLICT DO NOTHING;

-- =====================================================================================
-- 9. 2 000 000 постов: автор — пользователь n = g % 5000 + 1, по 400 на каждого, ~5 % DELETED.
-- =====================================================================================
INSERT INTO posts (id, author_id, title, content, status, created_at, updated_at)
    SELECT  gen_random_uuid(),
            u.id,
            'Пост ' || g,
            repeat(md5(g::TEXT), 10),
            CASE
                WHEN x.s < 0.05 THEN 'DELETED'
                ELSE 'PUBLISHED'
            END,
            x.ts,
            CASE
                WHEN x.s < 0.05 THEN LEAST(x.ts + INTERVAL '1 hour', now())
                ELSE x.ts
            END
        FROM generate_series(0, 1999999) g
            JOIN bench_users u ON u.n = g % 5000 + 1
            CROSS JOIN LATERAL (
                SELECT  random() + g * 0                                          AS s,
                        now() - make_interval(secs => random() * 31536000 + g * 0) AS ts
            ) x;

-- =====================================================================================
-- 10. Уборка и статистика.
-- =====================================================================================
DROP TABLE bench_users;

ANALYZE friendship_offer, friendships, subscriptions, posts;

--rollback TRUNCATE TABLE friendships, friendship_offer, subscriptions, posts;
