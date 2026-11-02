# Задание 03. `FeedPersistenceAdapter`: `LATERAL` + курсор + двухфазное чтение

**Раздел README:** 2.3–2.6.
**Зависит от:** заданий 01, 02.

## Цель

Реализовать новый контракт `FeedRepository` запросом, стоимость которого — `подписки × limit` записей индекса, независимо от числа постов и глубины страницы.

## Контекст

- Прежняя реализация — `git show 38036d9:src/main/java/ru/job4j/socialmediaapi/posts/internal/infrastructure/persistence/adapter/FeedPersistenceAdapter.java`. Оттуда берутся: `@Repository`, `@RequiredArgsConstructor`, константа `ROW_MAPPER` (те же колонки), поля `jdbcTemplate` и `mapper`.
- Почему так (замеры, планы) — README, разделы 1 и 2.
- Первые `limit` постов ленты обязательно входят в объединение первых `limit` постов каждого автора — поэтому `LIMIT` внутри `LATERAL` не теряет записей.
- Запрос читает таблицу `subscriptions` модуля `subscriptions` — задокументированное исключение (`docs/architecture.md`), Java-зависимостей между модулями не появляется.

Пакет — `ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.adapter`.

## Что сделать

1. Две SQL-константы (текстовые блоки, без склейки строк в рантайме):

   `FIND_FIRST_PAGE`:
   ```sql
   WITH page AS (
       SELECT lp.id
       FROM subscriptions s
       CROSS JOIN LATERAL (
           SELECT p.id, p.created_at
           FROM posts p
           WHERE p.author_id = s.followed_id
             AND p.status = 'PUBLISHED'
           ORDER BY p.created_at DESC, p.id DESC
           LIMIT ?
       ) lp
       WHERE s.follower_id = ?
       ORDER BY lp.created_at DESC, lp.id DESC
       LIMIT ?
   )
   SELECT p.id, p.author_id, p.title, p.content, p.status, p.created_at, p.updated_at
   FROM page
   JOIN posts p ON p.id = page.id
   ORDER BY p.created_at DESC, p.id DESC
   ```
   Параметры: `fetch`, `followerId`, `fetch`.

   `FIND_PAGE_AFTER` — то же, во внутреннем `WHERE` добавить:
   ```sql
             AND (p.created_at, p.id) < (?, ?)
   ```
   Параметры: `Timestamp.from(after.createdAt())`, `after.id()`, `fetch`, `followerId`, `fetch`.

   - `p.status = 'PUBLISHED'` во внутреннем запросе обязателен дословно: только так планировщик выбирает частичный индекс из задания 01.
   - Внутренний запрос выбирает только `id` и `created_at` (всё есть в индексе), `content` читается только для итоговых строк.
   - Внешний `ORDER BY` нужен: `JOIN` не гарантирует порядок из CTE.
2. Метод выборки страницы (private), общий для обоих публичных методов:
   - `fetch = limit + 1`;
   - `rows = jdbcTemplate.query(sql, ROW_MAPPER, args...)`;
   - если `rows.size() > limit` — на страницу идут первые `limit` строк, `next = Optional.of(mapper.toCursor(<последняя строка страницы>))`; иначе — все строки и `Optional.empty()`;
   - `posts` — через `mapper::toResponse`.
3. Проверка аргументов до обращения к БД (вынести в private-метод): `limit < 1 || limit > FeedRepository.MAX_LIMIT` → `IllegalArgumentException` с английским сообщением через `"...".formatted(limit)`; в `findFeedAfter` — `Objects.requireNonNull(after, "after")`.
4. Javadoc класса: лента — один запрос `subscriptions` × `LATERAL` по частичному индексу, курсор `(created_at, id)`, `limit + 1` для признака следующей страницы, ссылка на `docs/process/Feed.md`.

## Проверка

- `./mvnw clean compile`.
- По желанию — `EXPLAIN (ANALYZE, BUFFERS)` обоих запросов на локальной базе: `Index Only Scan using idx_posts_published_author_created`, во второй — `Index Cond: ((author_id = s.followed_id) AND (ROW(created_at, id) < ROW(...)))`; нет `Seq Scan on posts` и `external merge`.
- Тесты — задание 04.

## Критерии приёмки

- В SQL нет `OFFSET`; все значения передаются параметрами `?`.
- Нет check-then-act и лишних запросов: одна страница — один SQL-запрос, без `COUNT`.
- Маппинг — только через `FeedPersistenceMapper` (`ROW_MAPPER` для `ResultSet` — разрешённое исключение).
