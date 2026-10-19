--liquibase formatted sql

-- Примеры для ручной проверки ограничений таблицы posts (не выполняются миграцией:
-- комментарии стоят до --changeset и не входят в changeset, его checksum не меняется).
--
-- Проходят: допустимые статусы, а без status — значение по умолчанию PUBLISHED.
-- INSERT INTO posts (id, author_id, title, content, status, created_at, updated_at) VALUES (gen_random_uuid(), gen_random_uuid(), 'Первый пост', 'Текст', 'PUBLISHED', now(), now());
-- INSERT INTO posts (id, author_id, title, content, status, created_at, updated_at) VALUES (gen_random_uuid(), gen_random_uuid(), 'Удалённый пост', 'Текст', 'DELETED', now(), now());
-- INSERT INTO posts (id, author_id, title, content, created_at, updated_at) VALUES (gen_random_uuid(), gen_random_uuid(), 'Статус по умолчанию', 'Текст', now(), now());
--
-- Не проходят: ERROR: new row for relation "posts" violates check constraint "chk_posts_status".
-- INSERT INTO posts (id, author_id, title, content, status, created_at, updated_at) VALUES (gen_random_uuid(), gen_random_uuid(), 'Черновик', 'Текст', 'NEW', now(), now());
-- INSERT INTO posts (id, author_id, title, content, status, created_at, updated_at) VALUES (gen_random_uuid(), gen_random_uuid(), 'Оплаченный', 'Текст', 'PAID', now(), now());
-- INSERT INTO posts (id, author_id, title, content, status, created_at, updated_at) VALUES (gen_random_uuid(), gen_random_uuid(), 'Регистр', 'Текст', 'published', now(), now());
--
-- Не проходит: ERROR: null value in column "status" ... violates not-null constraint
-- (явный NULL не заменяется значением по умолчанию).
-- INSERT INTO posts (id, author_id, title, content, status, created_at, updated_at) VALUES (gen_random_uuid(), gen_random_uuid(), 'Без статуса', 'Текст', NULL, now(), now());

--changeset student:007
CREATE TABLE posts
(
    id              UUID            PRIMARY KEY,
    author_id       UUID            NOT NULL,
    title           VARCHAR(255)    NOT NULL,
    content         TEXT            NOT NULL,
    status          VARCHAR(16)     NOT NULL DEFAULT 'PUBLISHED',
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL,
    CONSTRAINT chk_posts_status CHECK (status IN ('PUBLISHED', 'DELETED'))
);

--rollback DROP TABLE IF EXISTS posts;
