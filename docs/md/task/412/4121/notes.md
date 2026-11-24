# 4121 — заметки анализа

## Урок vs текущая схема

| Что | Урок 4121 | У нас | Итог |
|---|---|---|---|
| master changelog | `db/changelog/db.changelog-master.yaml` | `db/db.changelog-master.yaml` | вопрос 1 |
| каталог скриптов | `db/changelog/changes/` | `db/changes/` | вопрос 1 |
| таблица заявок | `offer_friendships` | `friendship_offer` | не меняем (Р-1) |
| ссылка на заявку в `friendships` | `offer_friendship_id` | `friendship_offer_id` | вопрос 2 |
| `friendships.status` (ACTIVE/DELETED), FK на заявку | нет | есть | лишнее против урока, не мешает |
| уникальность пары заявок | `UNIQUE (from_user_id, to_user_id)` | 009: уникальный индекс `LEAST/GREATEST` (пара без учёта направления), constraint удалён | вопрос 4 (влияет на 4122) |
| индексы | 002, 004, 006 | те же + 008 (posts), 010, 011 (страницы с `id DESC`) | ок |
| rollback | `DROP TABLE x;` без `IF EXISTS` | `DROP ... IF EXISTS` во всех файлах | вопрос 3 |
| `IF EXISTS` в теле | нет | 011: `DROP INDEX IF EXISTS` ×5 | вопрос 3 |

## Нужно ли менять скрипты из-за JPA — нет

Проверено по уроку 4122 (сущность `OfferFriendshipEntity`) и всем таблицам:
- `id UUID PRIMARY KEY` — `@Id @GeneratedValue(strategy = UUID)` генерирует id в Java, sequence/DEFAULT в БД не нужны.
- `status VARCHAR(16)` + CHECK — `@Enumerated(EnumType.STRING) @Column(length = 16)`, имена совпадают с CHECK.
- `TIMESTAMPTZ` — `Instant` (Hibernate 7 маппит в `timestamp with time zone`), `ddl-auto: validate` проходит.
- `posts.content TEXT` — `String` (`@Column(columnDefinition = "text")` или без него: драйвер PG отдаёт TEXT как VARCHAR).
- `DEFAULT 'ACTIVE'` / `DEFAULT 'PUBLISHED'` — JPA всегда пишет значение поля, DEFAULT не мешает.
- Имена таблиц/колонок, отличные от урока, задаются в `@Table(name)` / `@Column(name)` — схема не меняется.
- Таблица `users` не нужна (Keycloak), FK на неё нет — как в уроке.
- `@Version` (оптимистическая блокировка) урок не вводит — колонка `version` не нужна.

Единственное место, где схема и урок 4122 не стыкуются: нативная вставка `ON CONFLICT (from_user_id, to_user_id)`
требует constraint/индекс ровно на эти колонки, а у нас он удалён в 009. Это правится в запросе 4122
(`ON CONFLICT DO NOTHING` без цели — как сейчас в JDBC-адаптере), а не в скрипте — вопрос 4.

## Checksum Liquibase
- Строки `--rollback` в formatted SQL в checksum changeSet не входят: их правка не ломает уже применённые миграции.
- Правка тела применённого changeSet (011) меняет checksum → `validation failed` на существующей БД
  (локальная `socialmediaapi-postgres-1`, порт 5435). Testcontainers не затрагивает — там БД каждый раз новая.
- Перенос файлов в другой каталог меняет `filename` в `databasechangelog` → Liquibase считает changeSet новыми
  и пытается выполнить `CREATE TABLE` повторно.
