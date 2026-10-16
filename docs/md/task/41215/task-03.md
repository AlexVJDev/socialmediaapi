# Задание 03. Порт `PostRepository`, `PostRow`, маппер и каркас адаптера

**Пункт задания:** 3.
**Зависит от:** заданий 01–02.

## Цель

Завести типы для публикаций по слоям гексагональной архитектуры проекта и пустой JDBC-адаптер, который реализуется в задании 04.

## Контекст

Слои и соглашения — `../../../../CLAUDE.md` (Architecture, «Маппинг»), образцы — подписки и заявки:

| Слой | Образец | Для публикаций |
| --- | --- | --- |
| `domain.<aggregate>` | `domain.friendshipoffer.FriendshipOfferStatus` | `domain.post.PostStatus` |
| `application.port.out` | `SubscriptionRepository` (вложенные `*Request` / `*Response` records) | `PostRepository` |
| `adapter.out.persistence.row` | `SubscriptionRow` | `PostRow` |
| `adapter.out.persistence.mapper` | `SubscriptionPersistenceMapper` | `PostPersistenceMapper` |
| `adapter.out.persistence.jdbc` | `SubscriptionRepositoryJdbc` | `PostRepositoryJdbc` |

- Право изменения будет защищено Keycloak позже, но **контракт порта уже принимает `actorId`** — id пользователя, который выполняет действие.
- Время и id не генерируются в адаптере: их передаёт вызывающий (будущий сервис возьмёт время из `Clock`).

## Что сделать

1. `domain.post.PostStatus` — `enum { PUBLISHED, DELETED }`.
2. `application.port.out.PostRepository`:
   ```java
   public interface PostRepository {

       CreatePostResponse createPost(CreatePostRequest request);

       Optional<FindPostResponse> findPublishedById(UUID postId);

       Optional<UpdatePostResponse> updatePost(UpdatePostRequest request);

       boolean deletePost(DeletePostRequest request);

       record CreatePostRequest(UUID id, UUID authorId, String title, String content,
                                PostStatus status, Instant createdAt, Instant updatedAt) { }

       record CreatePostResponse(UUID id, UUID authorId, String title, String content,
                                 PostStatus status, Instant createdAt, Instant updatedAt) { }

       record FindPostResponse(UUID id, UUID authorId, String title, String content,
                                 PostStatus status, Instant createdAt, Instant updatedAt) { }

       record UpdatePostRequest(UUID postId, UUID actorId, String title, String content, Instant updatedAt) { }

       record UpdatePostResponse(UUID id, UUID authorId, String title, String content,
                                 PostStatus status, Instant createdAt, Instant updatedAt) { }

       record DeletePostRequest(UUID postId, UUID actorId, Instant deletedAt) { }
   }
   ```
   - Если `*Response` совпадают один в один — допустимо один общий `PostResponse`; решение зафиксировать в Javadoc (как в `FriendshipOfferRepository`).
   - Семантику каждого метода описать в Javadoc (см. задание 04); `@throws` — только то, что адаптер реально бросает.
3. `adapter.out.persistence.row.PostRow` — record, зеркало таблицы: `UUID id, UUID authorId, String title, String content, String status, Instant createdAt, Instant updatedAt`. `public` только ради пакета `jdbc`; за пределы `adapter.out.persistence` не выходит.
4. `adapter.out.persistence.mapper.PostPersistenceMapper` (MapStruct, `@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)`): `toRow(CreatePostRequest)`, `toCreateResponse(PostRow)`, `toFindResponse(PostRow)`, `toUpdateResponse(PostRow)`. Enum ↔ `String` MapStruct делает сам.
5. `adapter.out.persistence.jdbc.PostRepositoryJdbc implements PostRepository` (`@Repository`, `JdbcTemplate` + маппер через конструктор): `RowMapper<PostRow>` для `ResultSet`. Методы — в задании 04.
   - `RowMapper<PostRow>` — константа `private static final RowMapper<PostRow> ROW_MAPPER` в самом адаптере, как в `FriendshipsRepositoryJdbc`.
6. Юнит-тест `PostPersistenceMapperTest` через `Mappers.getMapper(...)` (образец — `SubscriptionPersistenceMapperTest` / `FriendshipOfferPersistenceMapperTest`): enum → строка, строка → enum, неизвестный статус → `IllegalArgumentException`.

## Критерии приёмки

- `./mvnw clean compile` без предупреждений MapStruct об unmapped properties.
- `PostRow` не используется вне `adapter.out.persistence`; `domain` не зависит ни от чего.
- `./mvnw test -Dtest=PostPersistenceMapperTest` зелёный.
