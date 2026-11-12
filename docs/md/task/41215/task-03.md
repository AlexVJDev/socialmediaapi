# Задание 03. Порт `PostRepository`, `PostRow`, маппер и каркас адаптера

**Пункт задания:** 3.
**Зависит от:** заданий 01–02.

## Цель

Завести типы для публикаций по слоям модуля `posts` (DDD + гексагональная структура) и пустой JDBC-адаптер, который реализуется в задании 04.

## Контекст

Слои и соглашения — `../../CLAUDE2_backup.md` (Architecture, «Маппинг») и `../../../architecture.md`. Модуль — пакет `ru.job4j.socialmediaapi.posts` с `@ApplicationModule` в `package-info.java` (`allowedDependencies = {}`); пакеты ниже — относительно `posts.internal`. Образцы — модули `subscriptions` и `friendships`:

| Слой | Образец | Для публикаций |
| --- | --- | --- |
| `domain.model` | `friendships.internal.domain.model.FriendshipOfferStatus` | `PostStatus` |
| `application.port` | `subscriptions…application.port.SubscriptionRepository` | `PostRepository` |
| `application.port.model` | `*Request` / `*Response` records портов подписок и заявок | `CreatePostRequest`, `UpdatePostRequest`, `DeletePostRequest`, `PostResponse` |
| `infrastructure.persistence.row` | `SubscriptionRow` | `PostRow` |
| `infrastructure.persistence.mapper` | `SubscriptionPersistenceMapper` | `PostPersistenceMapper` |
| `infrastructure.persistence.adapter` | `SubscriptionPersistenceAdapter` | `PostPersistenceAdapter` |

- Право изменения будет защищено Keycloak позже, но **контракт порта уже принимает `actorId`** — id пользователя, который выполняет действие.
- Время и id не генерируются в адаптере: их передаёт вызывающий (будущий сценарий возьмёт время из `Clock`).

## Что сделать

1. `domain.model.PostStatus` — `enum { PUBLISHED, DELETED }`.
2. `application.port.PostRepository`:
   ```java
   public interface PostRepository {

       PostResponse createPost(CreatePostRequest request);

       Optional<PostResponse> findPublishedById(UUID postId);

       Optional<PostResponse> updatePost(UpdatePostRequest request);

       boolean deletePost(DeletePostRequest request);
   }
   ```
   Модели порта — отдельные records в `application.port.model`:
   ```java
   public record CreatePostRequest(UUID id, UUID authorId, String title, String content,
                                   PostStatus status, Instant createdAt, Instant updatedAt) { }

   public record UpdatePostRequest(UUID postId, UUID actorId, String title, String content,
                                   Instant updatedAt) { }

   public record DeletePostRequest(UUID postId, UUID actorId, Instant deletedAt) { }

   public record PostResponse(UUID id, UUID authorId, String title, String content,
                              PostStatus status, Instant createdAt, Instant updatedAt) { }
   ```
   - Один общий `PostResponse` для создания, чтения и изменения: поля ответов совпадают один в один; решение зафиксировано в Javadoc порта (как в `FriendshipOfferRepository`).
   - Семантику каждого метода описать в Javadoc (см. задание 04); `@throws` — только то, что адаптер реально бросает.
3. `infrastructure.persistence.row.PostRow` — record, зеркало таблицы: `UUID id, UUID authorId, String title, String content, String status, Instant createdAt, Instant updatedAt`. `public` только ради пакета `adapter`; за пределы `infrastructure.persistence` не выходит (проверяет `ApplicationModulesTest`).
4. `infrastructure.persistence.mapper.PostPersistenceMapper` (MapStruct, `@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)`): `toRow(CreatePostRequest)`, `toResponse(PostRow)`. Enum ↔ `String` MapStruct делает сам.
5. `infrastructure.persistence.adapter.PostPersistenceAdapter implements PostRepository` (`@Repository`, Lombok `@RequiredArgsConstructor`, `JdbcTemplate` + маппер): `RowMapper<PostRow>` для `ResultSet`. Методы — в задании 04.
   - `RowMapper<PostRow>` — константа `private static final RowMapper<PostRow> ROW_MAPPER` в самом адаптере, как в `FriendshipsPersistenceAdapter`.
6. Юнит-тест `PostPersistenceMapperTest` (тот же пакет под `src/test`) через `Mappers.getMapper(...)` (образец — `FriendshipOfferPersistenceMapperTest`): enum → строка, строка → enum, неизвестный статус → `IllegalArgumentException`.

## Критерии приёмки

- `./mvnw clean compile` без предупреждений MapStruct об unmapped properties.
- `PostRow` не используется вне `infrastructure.persistence`; `domain` не зависит ни от чего.
- `./mvnw test -Dtest=PostPersistenceMapperTest` и `./mvnw test -Dtest=ApplicationModulesTest` зелёные.
