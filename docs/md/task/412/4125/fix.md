Режим Ментора: ДА

Общие решения — `../4121/decisions.md`, JPA — `../4122/decisions.md`, `../4124/decisions.md`. Анализ — `notes.md`.

## Раунд 1
Ответы перенесены в decisions.md Р5-1…Р5-8.

## К сведению
- Миграция `posts` (п. 1 задания) уже есть: `007-create-posts.sql` (CHECK статуса, rollback) и `008-create-posts-indexes.sql`, подключены в master. Новых миграций нет.
- `content TEXT` ↔ `String`: драйвер PostgreSQL отдаёт `text` как `VARCHAR`, `ddl-auto: validate` проходит без `columnDefinition`.
- Замечание ментора в уроке про `*Command` — «Понял. Хорошо. Оставь».
