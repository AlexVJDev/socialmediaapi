# Задание 02. Контракт порта ленты с курсором и маппер

**Раздел README:** 2.1, 2.6, 2.7.
**Зависит от:** —.

## Цель

Заменить контракт `FeedRepository` с `limit` / `offset` на курсорный: первая страница и страница «после последнего показанного поста», плюс признак конца ленты.

## Контекст

- Удалённые классы (`FeedRepository`, `FeedPersistenceMapper`, `FeedPersistenceMapperTest`) можно посмотреть в коммите `38036d9`: `git show 38036d9:<путь>`. Javadoc, оформление и тест маппера берутся оттуда и дополняются.
- Остаются без изменений: `application.port.model.FeedPostResponse`, `infrastructure.persistence.row.PostRow`.
- Сервиса и REST у ленты нет — метод с `offset` не сохраняется.
- Курсор — пара `(createdAt, id)` последнего показанного поста. Он берётся только из ответа порта: `TIMESTAMPTZ` хранит микросекунды, а `Instant` из `Clock` может содержать наносекунды — собранный вручную курсор может не совпасть ни с одной строкой.

Пакеты — относительно `ru.job4j.socialmediaapi.posts.internal`.

## Что сделать

1. `application.port.model.FeedCursor`:
   ```java
   public record FeedCursor(Instant createdAt, UUID id) {
       public FeedCursor {
           Objects.requireNonNull(createdAt, "createdAt");
           Objects.requireNonNull(id, "id");
       }
   }
   ```
   Javadoc: позиция последнего показанного поста; берётся только из `FeedPageResponse.next`.
2. `application.port.model.FeedPageResponse`:
   ```java
   public record FeedPageResponse(List<FeedPostResponse> posts, Optional<FeedCursor> next) { }
   ```
   - `posts` — от новых к старым, не больше `limit`;
   - `next` — курсор следующей страницы (позиция последнего поста `posts`); пустой — дальше постов нет.
   - В compact constructor: `posts = List.copyOf(posts)`, `Objects.requireNonNull(next)`.
3. `application.port.FeedRepository`:
   ```java
   public interface FeedRepository {

       /** Maximum page size: a page costs subscriptions × limit index entries. */
       int MAX_LIMIT = 100;

       FeedPageResponse findFeed(UUID followerId, int limit);

       FeedPageResponse findFeedAfter(UUID followerId, FeedCursor after, int limit);
   }
   ```
   Javadoc (английский, как в проекте; Checkstyle требует `@param`):
   - что входит в ленту — как было (только `PUBLISHED`, только авторы из подписок, своих постов нет), порядок `created_at DESC, id DESC`;
   - `findFeedAfter` — посты строго после `after` в этом порядке; посты, опубликованные после чтения первой страницы, в следующие страницы не попадают;
   - пустой `next` = конец ленты; пустая лента — `posts` пуст, `next` пуст;
   - `@throws IllegalArgumentException` — `limit < 1` или `limit > MAX_LIMIT`;
   - `@throws NullPointerException` — `after == null` (если адаптер проверяет явно).
4. `infrastructure.persistence.mapper.FeedPersistenceMapper` — вернуть `toResponse(PostRow)` и добавить:
   ```java
   FeedCursor toCursor(PostRow row);
   ```
   Поля `createdAt` и `id` совпадают по именам; лишние поля источника `unmappedTargetPolicy` не затрагивают. Javadoc класса: `status` хранится строкой → `valueOf()`; `toCursor` — позиция строки в ленте.
5. `FeedPersistenceMapperTest` (тот же пакет в `src/test`) — вернуть два теста `toResponse` из `38036d9` и добавить:
   - `toCursor`: `createdAt` и `id` переносятся из Row (`assertThat(cursor).isEqualTo(new FeedCursor(...))`).
   - (необязательно) `FeedCursor` с `null` → `NullPointerException`; `FeedPageResponse.posts()` неизменяемый.

## Проверка

- `./mvnw clean compile` — без предупреждений MapStruct; сгенерированный `FeedPersistenceMapperImpl` в `target/generated-sources/annotations`.
- `./mvnw test -Dtest=FeedPersistenceMapperTest` (без Docker).

## Критерии приёмки

- В `FeedRepository` нет параметра `offset`.
- Все новые классы — в `application.port.model` / `infrastructure.persistence.mapper` модуля `posts`, Javadoc с `@param` на каждом record и методе.
