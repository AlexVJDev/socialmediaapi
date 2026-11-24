# 4126 — заметки анализа

Урок: лента строится при чтении из `subscriptions` + `posts`; `FeedRepository extends Repository<PostEntity, UUID>`
с нативным запросом `JOIN … ORDER BY created_at DESC, id DESC LIMIT :limit OFFSET :offset`; миграция индекса
`(author_id, created_at DESC, id DESC)`; один `@DataJpaTest`. Рассуждения о нагрузке (fan-out on read / write, `feed_items`,
популярные авторы) и замеры `EXPLAIN` — теория, не задание.

## Сравнение
| | Урок | У нас |
|---|---|---|
| пагинация | `LIMIT/OFFSET` | курсор `(created_at, id)` |
| план | `JOIN` всех постов подписок, сортировка | `CROSS JOIN LATERAL`: на подписку ≤ `limit + 1` записей индекса, затем чтение строк по PK |
| индекс | `(author_id, created_at DESC, id DESC)` | тот же, частичный `WHERE status = 'PUBLISHED'` (008) |
| интерфейс | `FeedRepository` | порт `FeedRepository` + `FeedJpaRepository` (Spring Data) |

## После 4126
- В `src/main` нет `JdbcTemplate`, `RowMapper`, `*Row`; все таблицы — через Spring Data JPA.
- `persistence.row` во всех модулях пуст → правило ArchUnit про `*Row` заменяется правилом про `*Entity`/`*JpaRepository`.
