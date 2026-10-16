# Задание 01. Миграция таблицы `posts`

**Пункт задания:** 1.

## Цель

Создать таблицу публикаций: первичный ключ, обязательные поля, допустимые состояния `PUBLISHED` и `DELETED`, блок `--rollback`.

## Контекст

- Миграции — Liquibase *formatted SQL* в `../../../../src/main/resources/db/changes`, подключаются в `db.changelog-master.yaml` строго по порядку. Последняя сейчас — `006-create-subscriptions-indexes.sql`, значит новая — `007`.
- По соглашению проекта таблица и её индексы — **разные файлы** (индекс — задание 02).
- Сервис не хранит пользователей: `author_id` — UUID пользователя из Keycloak, **без внешнего ключа** (как `follower_id` / `followed_id` в `subscriptions`).
- Все id — `UUID`, время — `TIMESTAMPTZ` (как в остальных таблицах).
- Уже применённые к локальной БД changeset'ы не редактировать (Liquibase упадёт на checksum).

## Что сделать

1. Файл `../../../../src/main/resources/db/changes/007-create-posts.sql`:
   ```sql
   --liquibase formatted sql

   --changeset student:007
   CREATE TABLE posts
   (
       id          UUID            PRIMARY KEY,
       author_id   UUID            NOT NULL,
       title       VARCHAR(255)    NOT NULL,
       content     TEXT            NOT NULL,
       status      VARCHAR(16)     NOT NULL DEFAULT 'PUBLISHED',
       created_at  TIMESTAMPTZ     NOT NULL,
       updated_at  TIMESTAMPTZ     NOT NULL,
       CONSTRAINT chk_posts_status CHECK (status IN ('PUBLISHED', 'DELETED'))
   );

   --rollback DROP TABLE IF EXISTS posts;
   ```
   - Длину `title` (255) и тип `content` (`TEXT`) зафиксировать в документации к таблице (см. ниже); если в курсе дана другая длина — взять её.
   - Проверку «заголовок не пустой» в БД не добавляем: это правило домена/валидации запроса, а не схемы.
2. Подключить файл в `db.changelog-master.yaml` после `006`:
   ```yaml
     - include:
         file: changes/007-create-posts.sql
         relativeToChangelogFile: true
   ```
3. Описать таблицу в `../../../../CLAUDE.md` (`../../CLAUDE2.md`, пункт **Persistence → Tables**): `posts` (`status` in `PUBLISHED|DELETED`, default `PUBLISHED`, `author_id` без FK).

## Проверка

- `./mvnw test` — Liquibase применяет миграцию в Testcontainers при старте контекста; существующие тесты зелёные.
- Rollback проверяется вручную на локальной БД (`docker compose up -d postgres`): `./mvnw liquibase:rollback -Dliquibase.rollbackCount=1` (если плагин не подключён — выполнить SQL из `--rollback` руками и убедиться, что он корректен).

## Критерии приёмки

- Таблица создаётся миграцией `007`, rollback удаляет её.
- `INSERT` со статусом вне `PUBLISHED|DELETED` падает на `chk_posts_status` (проверяется тестом в задании 05).
