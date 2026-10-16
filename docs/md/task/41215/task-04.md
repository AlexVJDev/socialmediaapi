# Задание 04. CRUD публикации в `PostRepositoryJdbc`

**Пункт задания:** 4.
**Зависит от:** задания 03.

## Цель

Реализовать в `PostRepositoryJdbc` создание, чтение, изменение и логическое удаление публикации. Изменять и удалять может только автор — это обеспечивает условие `WHERE`, а не проверка в коде.

## Контекст

- Нативный SQL через `JdbcTemplate`, путь данных как в других адаптерах: Request → Row (маппер) → SQL → Row (`RowMapper`) → Response (маппер).
- Чтение после записи — через `RETURNING`, как в `FriendshipOfferRepositoryJdbc.createFriendshipOffer`.
- **Без check-then-act.** Не делать «сначала `SELECT` автора, потом `UPDATE`»: между двумя запросами состояние может измениться. Проверка автора и статуса — внутри того же `UPDATE` (`WHERE id = ? AND author_id = ? AND status = 'PUBLISHED'`), это атомарно.
- Публикация с id от вызывающего (`UUID.randomUUID()`) — уникальный ключ на бизнес-поля не нужен, `*AlreadyExistsException` для постов не заводим.

## Что сделать

1. **Создание** — `createPost(CreatePostRequest)`:
   ```sql
   INSERT INTO posts (id, author_id, title, content, status, created_at, updated_at)
   VALUES (?, ?, ?, ?, ?, ?, ?)
   RETURNING id, author_id, title, content, status, created_at, updated_at
   ```
   `queryForObject(...)` + `RowMapper`. Нарушения `CHECK` / `NOT NULL` не переводить в доменные исключения — это ошибка в коде, `DataIntegrityViolationException` летит дальше (как в остальных адаптерах).
2. **Чтение** — `findPublishedById(UUID postId)`:
   ```sql
   SELECT id, author_id, title, content, status, created_at, updated_at
   FROM posts
   WHERE id = ? AND status = 'PUBLISHED'
   ```
   `jdbcTemplate.query(...).stream().findFirst()` (не `queryForObject` — он бросает исключение на пустом результате). Удалённая публикация читается как отсутствующая: и для автора, и для остальных («собственная или доступная» = любая опубликованная).
3. **Изменение** — `updatePost(UpdatePostRequest)`:
   ```sql
   UPDATE posts
   SET title = ?, content = ?, updated_at = ?
   WHERE id = ? AND author_id = ? AND status = 'PUBLISHED'
   RETURNING id, author_id, title, content, status, created_at, updated_at
   ```
   Пустой результат → `Optional.empty()`: публикации нет, она удалена или `actorId` — не автор. Адаптер эти случаи **не различает**; различать (404 vs 403) будет сервис, если понадобится, — отдельным чтением уже *после* неуспешного `UPDATE`, а не до него.
4. **Логическое удаление** — `deletePost(DeletePostRequest)`:
   ```sql
   UPDATE posts
   SET status = 'DELETED', updated_at = ?
   WHERE id = ? AND author_id = ? AND status = 'PUBLISHED'
   ```
   `jdbcTemplate.update(...)` → `true`, если изменена одна строка; `false` — нет публикации, не автор или уже удалена (повторное удаление ничего не меняет). Физический `DELETE` не делаем.
5. Javadoc порта: для каждого метода — что значит пустой результат / `false`; явно указать, что `actorId` должен совпадать с автором.

## Критерии приёмки

- Все `UPDATE` содержат `author_id = ?` в `WHERE` — один пользователь не может изменить или удалить публикацию другого.
- Ни один метод не делает чтение перед записью ради проверки прав или статуса.
- `./mvnw verify` зелёный (тесты — в задании 05).
