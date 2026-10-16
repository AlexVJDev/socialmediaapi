# Задание 02. Миграция индекса публикаций автора

**Пункт задания:** 2.
**Зависит от:** задания 01.

## Цель

Отдельной миграцией создать индекс `(author_id, created_at DESC, id DESC)` с rollback.

## Контекст

- Индекс поддерживает выборку публикаций конкретного автора в стабильном порядке «от новых к старым»: `WHERE author_id = ? ORDER BY created_at DESC, id DESC`.
- Он же помогает запросу ленты (задание 06): для каждого автора из подписок PostgreSQL читает его посты уже в нужном порядке.
- По соглашению проекта индексы таблицы — в отдельном файле (`002`, `004`, `006` — образцы).

## Что сделать

1. Файл `../../../../src/main/resources/db/changes/008-create-posts-indexes.sql`:
   ```sql
   --liquibase formatted sql

   --changeset student:008
   CREATE INDEX idx_posts_author_created ON posts (author_id, created_at DESC, id DESC);

   --rollback DROP INDEX IF EXISTS idx_posts_author_created;
   ```
2. Подключить в `db.changelog-master.yaml` после `007`.
3. Упомянуть индекс в `CLAUDE2.md` рядом с описанием таблицы `posts`.

## Почему не частичный индекс

Можно было бы сделать `... WHERE status = 'PUBLISHED'`: удалённые посты не попадают ни в ленту, ни в выдачу. Но задание требует индекс именно на `(author_id, created_at DESC, id DESC)`, а удалённых записей на этом этапе мало. Вариант с частичным индексом — отметить как возможное улучшение, не делать.

## Критерии приёмки

- `./mvnw test` зелёный (миграция применяется в Testcontainers).
- Rollback удаляет только индекс, таблица остаётся.
