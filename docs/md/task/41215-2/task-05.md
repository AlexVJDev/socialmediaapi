# Задание 05. Документация и итоговая проверка

**Раздел README:** 2.9, 3, 4.
**Зависит от:** заданий 01–04.

## Цель

Привести документацию к курсорной ленте и прогнать все проверки, которые выполняет CI.

## Что сделать

1. `docs/process/Feed.md`:
   - «Как строится» — новый SQL (`LATERAL`, двухфазное чтение), курсор `(created_at, id)` как основной способ страниц;
   - индексы: `idx_posts_published_author_created` (частичный, Index Only Scan) и `uq_subscriptions_users` для подписок читателя; убрать `idx_posts_author_created`;
   - «Страницы» — `findFeed` / `findFeedAfter`, `next`, `limit` от 1 до 100, курсор только из ответа (микросекунды `TIMESTAMPTZ`);
   - бывший раздел «Ограничения offset pagination» сделать историей решения: почему отказались от `OFFSET` + таблица замеров из README, раздел 1;
   - «Профиль нагрузки и выбор решения» — README, раздел 3.1: до 200 подписок у читателя, до миллионов подписчиков у автора; стоимость страницы `подписки × limit`, не зависит от числа подписчиков автора; таблица замеров A–D;
   - «Развитие» — README, раздел 4: почему не fan-out on write (миллионы вставок на пост популярного автора), реплики, кеш последних постов автора (инвалидация по автору, не по подписчикам), шардирование по `author_id`.
2. `docs/md/CLAUDE2.md`:
   - блок «Module `posts`» → «Feed»: новые методы, `FeedCursor`, `FeedPageResponse`, `MAX_LIMIT`, `limit + 1`, `LATERAL` и частичный индекс вместо `findFeed(followerId, limit, offset)` и `LIMIT ? OFFSET ?`;
   - «Persistence»: перечень changesets до `010` (`010` — частичный индекс `posts`), индекс `posts` — `idx_posts_published_author_created ... WHERE status = 'PUBLISHED'`;
   - упоминание плана задачи: `md/task/41215-2`.
3. `docs/architecture.md` — абзац об исключении для SQL ленты:
   - «одна выборка с сортировкой и курсорной пагинацией» вместо «пагинацией»;
   - фразу «При росте нагрузки лента переходит на собственную проекцию подписок, наполняемую событиями `subscriptions`» заменить: при профиле из README, раздел 3.1 (до 200 подписок у читателя, миллионы подписчиков у популярных авторов) лента остаётся сборкой при чтении; если `posts` и `subscriptions` разойдутся по разным базам — список авторов берётся через API `subscriptions` (вариант D), и исключение снимается.
4. `docs/md/shema.md` — перегенерировать: `python3 -I docs/md/scripts/gen-schema.py <путь к модулю posts>` (из корня проекта).
5. `docs/md/task/41215-2/README.md` — статусы шагов в разделе 5, раздел «Отличия от плана», если они появились.

## Проверка

```shell
./mvnw test -Dtest=ApplicationModulesTest   # архитектура, без Docker
./mvnw verify                               # все тесты + JaCoCo + Checkstyle (как в CI)
```

## Критерии приёмки

- `./mvnw verify` зелёный.
- В `src/main/resources/db/changes` нет эталонов `*-b.sql` (удаляются в задании 01).
- Во всей документации, кроме истории `docs/md/task/41215` и `docs/md/refactoring`, нет упоминаний `findFeed(followerId, limit, offset)`, `LIMIT ? OFFSET ?` для ленты и `idx_posts_author_created`.
