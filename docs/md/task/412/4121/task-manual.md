# 4121 — Модель данных социальной сети (Liquibase). План

Режим Ментора: ДА. Эталон — `decisions.md`; при расхождении плана с решением прав `decisions.md`.
Пункты выполняет Claude по одному, по команде пользователя; пользователь проверяет (Р-2).

Суть: схема уже соответствует уроку (таблицы, ограничения, индексы в отдельных скриптах, rollback в каждом файле).
Под JPA скрипты не меняются (Р1-5). Остаётся убрать `IF EXISTS` (Р-3, Р1-3) и проверить применение и откат миграций.

| Итого | Новые | Изменить |
|---|---|---|
| файлов | 0 | 11 (`db/changes/001…011-*.sql`) |

Не меняются: `db.changelog-master.yaml`, `application.yml`, `sql-examples-explain.sql`, `dev/swagger-examples.sql`, Java-код.

## Где будут файлы

```
src/main/resources/db/
├── db.changelog-master.yaml                        (без изменений)
└── changes/
    ├── 001-create-friendship-offer.sql             (изменить) --rollback
    ├── 002-create-friendship-offer-indexes.sql     (изменить) --rollback
    ├── 003-create-friendships.sql                  (изменить) --rollback
    ├── 004-create-friendships-indexes.sql          (изменить) --rollback
    ├── 005-create-subscriptions.sql                (изменить) --rollback
    ├── 006-create-subscriptions-indexes.sql        (изменить) --rollback
    ├── 007-create-posts.sql                        (изменить) --rollback
    ├── 008-create-posts-indexes.sql                (изменить) --rollback
    ├── 009-friendship-offer-unique-pair.sql        (изменить) --rollback
    ├── 010-friendship-offer-created-index.sql      (изменить) --rollback
    └── 011-friendships-page-indexes.sql            (изменить) тело + --rollback
```

## Файлы

| № | Файл | Слой | Действие | Решение |
|---|---|---|---|---|
| 1 | `001…010-*.sql` | БД / миграции | `--rollback DROP … IF EXISTS` → `DROP …` | Р-3, Р1-3 |
| 2 | `011-friendships-page-indexes.sql` | БД / миграции | `DROP INDEX IF EXISTS` → `DROP INDEX` в теле и в rollback | Р-3, Р1-3 |
| 3 | локальная БД | окружение | пересоздать, запустить приложение дважды | Р1-3, Р1-5 |
| 4 | локальная БД | окружение | rollback последнего changeSet и повторное применение | Р1-5 |

## 1. Rollback 001–010 без `IF EXISTS`
Зачем: при откате ошибка «объекта нет» должна быть видна, а не скрыта (Р-3).
Что сделать: в строках `--rollback` убрать `IF EXISTS`, порядок строк не менять. Пример (001):
```sql
--rollback DROP TABLE friendship_offer;
```
Тело changeSet не трогаем — checksum не меняется (строки `--rollback` в него не входят).

## 2. 011 без `IF EXISTS`
Зачем: тот же Р-3; все удаляемые индексы к моменту 011 гарантированно существуют (созданы в 002, 004, 010).
Что сделать: в теле — 5 × `DROP INDEX IF EXISTS x;` → `DROP INDEX x;`; в rollback — 4 × `DROP INDEX IF EXISTS` → `DROP INDEX`.
Последствие: checksum 011 меняется → на старой локальной БД запуск упадёт с `Validation Failed`; поэтому п. 3.

## 3. Чистая БД, применение и отсутствие повторного применения
```bash
docker compose down -v            # удаляет volume postgres_data (учебные данные)
docker compose up -d postgres
./mvnw spring-boot:run            # 1-й запуск
```
Проверить в логе: `Running Changeset: db/changes/001-…` … `011-…`, без ошибок.
Остановить и запустить ещё раз: строк `Running Changeset` для 001–011 нет (записаны в `databasechangelog`).
```bash
docker compose exec postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c "select id, filename, exectype from databasechangelog order by orderexecuted;"
```
Ожидается 11 строк `student:001…011`, `EXECUTED`. Запускать без `LIQUIBASE_CONTEXTS=dev`, иначе последним
окажется changeSet `swagger-examples`, и п. 4 откатит его, а не 011.

## 4. Rollback последнего changeSet (011) и повторное применение
Liquibase Maven-плагина в проекте нет — откат через Liquibase CLI в Docker, pom не меняем.
Путь changelog совпадает с тем, что пишет Spring Boot (`db/changes/…`), поэтому CLI видит те же changeSet.
```bash
docker run --rm --network host \
  -v "$PWD/src/main/resources:/liquibase/changelog" \
  -v ~/.m2/repository/org/postgresql/postgresql/42.7.14/postgresql-42.7.14.jar:/liquibase/lib/postgresql.jar:ro \
  liquibase/liquibase:5.0.3 \
  --search-path=/liquibase/changelog \
  --changelog-file=db/db.changelog-master.yaml \
  --url="jdbc:postgresql://localhost:${POSTGRES_PORT}/${POSTGRES_DB}" \
  --username="$POSTGRES_USER" --password="$POSTGRES_PASSWORD" \
  --contexts=default \
  rollback-count --count=1
```
Проверить индексы:
```bash
docker compose exec postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "\di"
```
Ожидается: нет `idx_friendships_*_page`; есть `idx_friendship_offer_created`, `idx_friendships_first_user`,
`idx_friendships_second_user`, `idx_friendship_offer_incoming/outgoing` (старые, без `id DESC`); в `databasechangelog` 10 строк.
Затем `./mvnw spring-boot:run` — в логе `Running Changeset: db/changes/011-…`, индексы снова как после п. 3.

Образ `liquibase/liquibase:5.0.3` — та же версия, что `liquibase-core` из Spring Boot 4.1.1.

## После выполнения
- `./mvnw test` — тесты на Testcontainers применяют миграции с нуля.
- Напоминание: `ApplicationModulesTest` — по команде пользователя.
- Коммит для ментора делает пользователь.
