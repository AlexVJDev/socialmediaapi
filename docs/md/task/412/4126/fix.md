Режим Ментора: ДА

Общие решения — `../4121/decisions.md`; JPA — `../4122/`, `../4124/`, `../4125/decisions.md`. Анализ — `notes.md`.

## Раунд 1
Ответы перенесены в decisions.md Р6-1…Р6-7.

## К сведению
- Замеры из урока (`EXPLAIN (ANALYZE, BUFFERS)`, медиана/p95) — не пункт задания; планы ленты уже разобраны в 41215-2 (`docs/process/Feed.md`, `0091-sql-examples.sql`).
- После 4126 зависимость `spring-boot-starter-jdbc` не нужна и так (её заменил data-jpa в 4122); `JdbcTemplate` остаётся только в тестах.
