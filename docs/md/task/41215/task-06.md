# Задание 06. `FeedRepository`: SQL ленты с `limit` / `offset`

**Пункты задания:** 5, 6.
**Зависит от:** заданий 03–04.

## Цель

Построить ленту читателя одним SQL-запросом из `subscriptions` и `posts` — без отдельной таблицы ленты — со стабильной сортировкой и постраничной выдачей через `limit` / `offset`.

## Контекст

Лента строится при чтении:

```
текущий пользователь
  → его подписки
  → опубликованные посты авторов
  → сортировка от новых к старым
  → страница результата
```

- В ленту не входят удалённые публикации и публикации авторов, на которых читатель не подписан. Собственные посты читателя тоже не входят: подписки на себя запрещены (`chk_subscriptions_different_users`).
- `FeedRepository` отделён от `PostRepository`: тот работает с одной публикацией, этот — строит список.
- Индексы: `idx_subscriptions_follower` (уже есть) и `idx_posts_author_created` (задание 02).

## Что сделать

1. `application.port.out.FeedRepository`:
   ```java
   public interface FeedRepository {

       /**
        * Published posts of the authors the follower is subscribed to, newest first.
        * ...
        */
       List<FeedPostResponse> findFeed(UUID followerId, int limit, int offset);

       record FeedPostResponse(UUID id, UUID authorId, String title, String content,
                               PostStatus status, Instant createdAt, Instant updatedAt) { }
   }
   ```
   (Если совпадает с `PostRepository.FindPostResponse` — можно переиспользовать, решение — в Javadoc.)
2. `adapter.out.persistence.jdbc.FeedRepositoryJdbc implements FeedRepository`:
   ```sql
   SELECT p.id, p.author_id, p.title, p.content, p.status, p.created_at, p.updated_at
   FROM subscriptions s
   JOIN posts p ON p.author_id = s.followed_id
   WHERE s.follower_id = ?
     AND p.status = 'PUBLISHED'
   ORDER BY p.created_at DESC, p.id DESC
   LIMIT ? OFFSET ?
   ```
   - `RowMapper<PostRow>` — своя константа `ROW_MAPPER` в `FeedRepositoryJdbc`, как в остальных адаптерах (колонки те же, что в `PostRepositoryJdbc`); маппинг `PostRow` → `FeedPostResponse` — метод MapStruct-маппера (`FeedPersistenceMapper` или `PostPersistenceMapper.toFeedResponse`), не вручную.
   - `id DESC` — второй ключ сортировки: у нескольких публикаций может совпасть `created_at`, без него порядок внутри одинакового времени не определён и страницы могут «перемешиваться».
   - Аргументы: `limit > 0`, `offset >= 0`, иначе `IllegalArgumentException` до обращения к БД (PostgreSQL на отрицательные значения ответит ошибкой синтаксиса выполнения, лучше упасть явно). Верхнюю границу `limit` (например, 100) задаст будущий REST-слой.
3. **Документация** ограничений — новый файл `../../../process/Feed.md` (на русском):
   - как строится лента (схема выше, SQL, почему без отдельной таблицы);
   - **ограничения offset pagination**:
     - `OFFSET N` заставляет БД прочитать и отбросить N строк — чем дальше страница, тем медленнее запрос на больших лентах;
     - на быстро меняющейся ленте страницы «плывут»: новый пост сверху сдвигает выдачу, и на следующей странице читатель видит дубль; удаление поста или отписка — пропуск записи;
   - дальнейшее развитие — **cursor (keyset) pagination**: следующая страница по последней паре `(created_at, id)`: `WHERE (p.created_at, p.id) < (?, ?) ORDER BY p.created_at DESC, p.id DESC LIMIT ?`; индекс из задания 02 подходит и для неё.
4. Ссылку на `../../../process/Feed.md` и описание `FeedRepository` добавить в `CLAUDE2.md` (Architecture → Persistence / Business process).

## Критерии приёмки

- Сортировка в SQL — ровно `created_at DESC, id DESC`, `limit` и `offset` передаются параметрами (`?`), а не конкатенацией строк.
- `./mvnw verify` зелёный (тесты — в задании 08).
