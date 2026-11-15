# 41225 — API публикаций и ленты. План для ручной реализации

Режим Ментора: ДА (Р0-1). Эталон — `decisions.md`; при расхождении плана с решением прав `decisions.md`.
Пункты с пометкой **(план)** — предложение плана, а не принятое решение: при несогласии поправим до начала работы.

**6 этапов + ручные тесты.** После каждого этапа `./mvnw verify` зелёный.

Итого: **~34 новых**, **~28 изменяемых** (из них 2 переименования) файлов.

| Этап | Новые | Изменить |
|---|---|---|
| 1. Текущий пользователь (shared) | 5 | 3 (+ securitySchemes в 3 спецификациях — этапы 2, 5) |
| 2. Контракт и генерация posts | 5 | 5 (1 переименование) |
| 3. posts: домен и сценарии | 13 | 3 |
| 4. posts: web | — | 7 |
| 5. subscriptions: подписка/отписка | 8 | 9 (1 переименование) |
| 6. Сценарный MockMvc-тест | 1 | 1 |
| 7. Ручные тесты | 2 + `bruno/41225/` | — |

## Где будут файлы

```
docs/api/
├── openapi.yaml                              (этапы 2, 5) пути + components.securitySchemes
├── modules/posts.yaml, modules/subscriptions.yaml  (этапы 2, 5)
├── responses/unauthorized.yaml               (этап 1, новый) общий ответ 401
├── schemas/post-request.yaml                 (этап 2, новый) тело POST/PUT
├── schemas/post-page.yaml, post-cursor.yaml  (этап 2, новые) страница постов (лента и посты автора)
├── schemas/subscription.yaml                 (этап 5, новый) вынесено из subscribes-get.yaml
└── paths/
    ├── post-create.yaml                      (этап 2, новый)  POST /api/v1/posts
    ├── post-get.yaml → post-by-id.yaml       (этап 2, переименовать) GET/PUT/DELETE
    ├── posts-by-user.yaml                    (этап 2, новый)  GET /api/v1/users/{userId}/posts
    ├── feed-get.yaml                         (этап 2)
    ├── subscribes-get.yaml → subscribes.yaml (этап 5, переименовать) GET + POST
    └── subscribe-delete.yaml                 (этап 5, новый)  DELETE /api/v1/subscribes/{userId}

src/main/java/ru/job4j/socialmediaapi/
├── shared/security/                          (этап 1, новые) CurrentUserProvider, HeaderCurrentUserProvider,
│                                                              UnauthenticatedException
├── shared/web/CommonErrorCode.java           (этап 1) + UNAUTHORIZED
├── shared/exception/GlobalExceptionHandler.java  (этап 1) + 401
├── posts/internal/domain/model/Post.java     (этап 3, новый)
├── posts/internal/application/...            (этап 3) команды, use case'ы, порт, маппер
├── posts/internal/infrastructure/...         (этапы 3, 4) адаптер, контроллеры, web-маппер, advice
└── subscriptions/internal/...                (этап 5)

src/test/java/ru/job4j/socialmediaapi/
├── shared/security/HeaderCurrentUserProviderTest.java   (этап 1, новый)
├── posts/internal/...                        (этапы 3, 4)
├── subscriptions/internal/application/usecase/SubscribeTest.java  (этап 5, новый)
└── PostFeedScenarioTest.java                 (этап 6, новый)
```

## Файлы

| № | Файл | Слой | Действие | Решение |
|---|---|---|---|---|
| **Этап 1** | | | | |
| 1.1 | `shared/security/CurrentUserProvider.java` | shared | новый | Р0-4, Р3-2 |
| 1.2 | `shared/security/UnauthenticatedException.java` | shared | новый | Р3-2 |
| 1.3 | `shared/security/HeaderCurrentUserProvider.java` | shared | новый | Р3-1 |
| 1.4 | `shared/web/CommonErrorCode.java` | shared | изменить | Р3-2 |
| 1.5 | `shared/exception/GlobalExceptionHandler.java` | shared | изменить | Р3-2 |
| 1.6 | `docs/api/responses/unauthorized.yaml` | контракт | новый | Р3-2 |
| 1.7 | `docs/api/openapi.yaml` — `components.securitySchemes` | контракт | изменить | Р3-4 |
| 1.8 | `HeaderCurrentUserProviderTest.java` | test | новый | Р3-1 |
| **Этап 2** | | | | |
| 2.1 | `docs/api/schemas/post-request.yaml` | контракт | новый | Р1-8, Р0-4 |
| 2.2 | `docs/api/schemas/post-page.yaml`, `post-cursor.yaml` | контракт | новые | Р1-4 (план) |
| 2.3 | `docs/api/paths/post-create.yaml` | контракт | новый | Р1-3, Р1-10, Р3-4 |
| 2.4 | `paths/post-get.yaml` → `post-by-id.yaml` | контракт | переименовать, изменить | Р1-3, Р1-7, Р1-10 |
| 2.5 | `docs/api/paths/posts-by-user.yaml` | контракт | новый | Р1-3, Р1-4, Р1-10 |
| 2.6 | `docs/api/paths/feed-get.yaml` | контракт | изменить | Р3-3, Р3-4 |
| 2.7 | `docs/api/openapi.yaml`, `docs/api/modules/posts.yaml` | контракт | изменить | Р1-2 |
| 2.8 | `pom.xml` | генерация | изменить | Р1-8 |
| **Этап 3** | | | | |
| 3.1 | `posts/internal/domain/model/Post.java` | domain | новый | Р1-6, Р1-7 |
| 3.2 | `application/model/` — 4 команды | application | новые | Р0-4 |
| 3.3 | `application/port/PostRepository.java` | application | изменить | Р1-4 |
| 3.4 | `application/mapper/PostApplicationMapper.java` | application | изменить | — |
| 3.5 | `application/usecase/` — `CreatePost`, `UpdatePost`, `DeletePost`, `GetUserPosts` | application | новые | Р1-6, Р1-7, Р1-10 |
| 3.6 | `persistence/adapter/PostPersistenceAdapter.java` | infrastructure | изменить | Р1-4 |
| 3.7 | тесты: `PostTest`, `CreatePostTest`, `UpdatePostTest`, `DeletePostTest`, `PostPersistenceAdapterTest` | test | новые / изменить | — |
| **Этап 4** | | | | |
| 4.1 | `web/mapper/PostWebMapper.java` | web | изменить | Р0-4 |
| 4.2 | `web/PostController.java` | web | изменить | Р0-4, Р1-10 |
| 4.3 | `web/FeedController.java` | web | изменить | Р3-3 |
| 4.4 | `web/PostExceptionHandler.java` | web | изменить | Р1-7, Р1-8 |
| 4.5 | `PostControllerTest`, `PostWebMapperTest` | test | изменить | — |
| 4.6 | `docs/process/Feed.md` | документация | изменить | Р3-3 |
| **Этап 5** | | | | |
| 5.1 | `schemas/subscription.yaml`; `subscribes-get.yaml` → `subscribes.yaml`; `subscribe-delete.yaml` | контракт | новые / переименовать | Р1-5, Р2-3, Р0-3 (план — имена) |
| 5.2 | `openapi.yaml`, `modules/subscriptions.yaml` | контракт | изменить | Р1-5 |
| 5.3 | `pom.xml` | генерация | изменить | Р1-5 |
| 5.4 | `application/model/` — `SubscribeCommand`, `UnsubscribeCommand`, `SubscribeResult` | application | новые | Р2-3 |
| 5.5 | `application/port/SubscriptionRepository.java` + адаптер | application / infrastructure | изменить | Р2-3 |
| 5.6 | `application/usecase/Subscribe.java`, `Unsubscribe.java` | application | новые | Р2-3 |
| 5.7 | `SubscriptionController`, `SubscriptionWebMapper`, `SubscriptionExceptionHandler`, `SubscriptionApplicationMapper` | web / application | изменить | Р0-3, Р2-3 |
| 5.8 | `SubscribeTest.java` | test | новый | — |
| **Этап 6** | | | | |
| 6.1 | `PostFeedScenarioTest.java` | test | новый | Р1-5, Р3-4 |
| 6.2 | `FriendshipFeedTest.java` — Javadoc | test | изменить | — |
| **Этап 7** | | | | |
| 7.1 | `manual-test-curl.md`, `manual-test-bruno.md`, `bruno/41225/` | docs | новые | — |

---

## Этап 1. Текущий пользователь

**1.1 `shared/security/CurrentUserProvider.java`** — порт «кто выполняет запрос» (Р0-4).
```java
public interface CurrentUserProvider {
    /** @throws UnauthenticatedException пользователь не определён */
    UUID currentUserId();
}
```
Javadoc: сейчас реализация — заголовок `X-User-Id`, после Keycloak — `sub` из JWT; меняется только реализация.

**1.2 `UnauthenticatedException`** — `RuntimeException`, сообщение без значения заголовка.

**1.3 `HeaderCurrentUserProvider`** — `@Component implements CurrentUserProvider` (Р3-1).
- Запрос — через `RequestContextHolder.currentRequestAttributes()` → `ServletRequestAttributes.getRequest()` (бин singleton, request не внедряем полем).
- `getHeader("X-User-Id")`; `null`/пусто → `UnauthenticatedException`; `UUID.fromString` с `IllegalArgumentException` → `UnauthenticatedException`.
- Имя заголовка — константа `HEADER = "X-User-Id"` (её же используют тесты).

**1.4 `CommonErrorCode`** — `UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Пользователь не определён")`.

**1.5 `GlobalExceptionHandler`** — `@ExceptionHandler(UnauthenticatedException.class)` → `CommonErrorCode.UNAUTHORIZED.response()`, без лога ошибки (это не баг).
Модульные advice его не перехватывают — общий обработчик достаточен.

**1.6 `responses/unauthorized.yaml`** — по образцу `internal-error.yaml`: `401`, `application/problem+json`, пример `{status: 401, detail: …, code: UNAUTHORIZED}`.

**1.7 `openapi.yaml`** — добавить:
```yaml
components:
  securitySchemes:
    userIdHeader:
      type: apiKey
      in: header
      name: X-User-Id
      description: Временная замена токена до подключения Keycloak
```
В операциях, которым нужен пользователь, — `security: [ { userIdHeader: [] } ]` (этапы 2, 5). Генератор по `security` параметров не добавляет — сигнатуры `*Api` не меняются. Тот же блок `components` — в `modules/posts.yaml` и `modules/subscriptions.yaml` (иначе генератор предупреждает о неизвестной схеме).

**1.8 `HeaderCurrentUserProviderTest`** — unit: `MockHttpServletRequest` с заголовком + `RequestContextHolder.setRequestAttributes(...)` → UUID; без заголовка → `UnauthenticatedException`. В `@AfterEach` — `RequestContextHolder.resetRequestAttributes()`.

---

## Этап 2. Контракт и генерация posts

**2.1 `schemas/post-request.yaml`** (Р1-8):
```yaml
type: object
required: [title, content]
properties:
  title:   { type: string, minLength: 1, maxLength: 255,   pattern: '(?s).*\S.*', example: Первый пост }
  content: { type: string, minLength: 1, maxLength: 10000, pattern: '(?s).*\S.*', example: Содержимое публикации }
```
`(?s)` обязателен: `@Pattern` проверяет всю строку (`matches`), а `content` бывает многострочным. `authorId` в схеме нет (Р0-4).

**2.2 `schemas/post-page.yaml`, `post-cursor.yaml`** **(план)** — вынести из `feed-get.yaml` инлайн-схемы страницы (`{posts, next}`) и курсора, чтобы лента и посты автора ссылались на одну схему. Сгенерированные `FeedPageWebResponse`/`FeedCursorWebResponse` станут `PostPageWebResponse`/`PostCursorWebResponse` (правка `PostWebMapper` в 4.1).

**2.3 `paths/post-create.yaml`** — `post`, tag `Post`, `operationId: createPost`, `security: userIdHeader`, тело `post-request.yaml`.
Ответы: `201` (`headers: Location`, тело `post.yaml`), `400 VALIDATION_ERROR`, `401` → `unauthorized.yaml`, `500`.

**2.4 `post-get.yaml` → `post-by-id.yaml`** — существующий `get` без изменений (публичный) + 
- `put`: `operationId: updatePost`, `security`, тело `post-request.yaml`; `200` (`post.yaml`), `400`, `401`, `404 POST_NOT_FOUND` (нет, удалён или чужой — Р1-7), `500`.
- `delete`: `operationId: deletePost`, `security`; `204`, `400`, `401`, `404`, `500`.
Ссылку в `openapi.yaml` и `modules/posts.yaml` поправить на новое имя.

**2.5 `paths/posts-by-user.yaml`** — `get`, tag `Post`, `operationId: getUserPosts`, без `security` (публичный).
Параметры: path `userId` (uuid) + `limit`, `afterCreatedAt`, `afterId` — как в `feed-get.yaml`. `200` → `post-page.yaml` (неизвестный пользователь — пустая страница, Р1-10), `400`, `500`.

**2.6 `feed-get.yaml`** — удалить параметр `followerId` (Р3-3), добавить `security` и `401`, `200` → `$ref: '../schemas/post-page.yaml'`; в `description` — «лента текущего пользователя».

**2.7 `openapi.yaml`, `modules/posts.yaml`** — пути (Р1-2):
```yaml
  /api/v1/posts:                  { $ref: './paths/post-create.yaml' }
  /api/v1/posts/{postId}:         { $ref: './paths/post-by-id.yaml' }
  /api/v1/users/{userId}/posts:   { $ref: './paths/posts-by-user.yaml' }
  /api/v1/feed:                   { $ref: './paths/feed-get.yaml' }
```

**2.8 `pom.xml`** — по образцу friendships:
- `openapi.posts.requests` = `PostWebRequest`; `model-names` += `post-request=PostWebRequest,post-page=PostPageWebResponse,post-cursor=PostCursorWebResponse`.
- `openapi.posts.responses` = `post,post-page,post-cursor` (имена файлов, не mapped — см. комментарий в pom); инлайн-имена `getFeed_200_*` убрать.
- `imports` += `PostWebRequest=...web.request.PostWebRequest`, `PostPageWebResponse`, `PostCursorWebResponse` (`...web.response`); убрать `FeedPageWebResponse`, `FeedCursorWebResponse`.
- Новый execution `posts-requests` (копия `friendships-requests` с `openapi.posts.*`). Комментарий «only GET operations… no posts-requests» обновить.

Проверка: `./mvnw generate-sources` → в `PostApi` методы `createPost(PostWebRequest)`, `updatePost(UUID, PostWebRequest)`, `deletePost(UUID)`, `getUserPosts(...)`; в `FeedApi.getFeed` нет `followerId`. Компиляция контроллеров упадёт до этапа 4 — этапы 2–4 проверяются сборкой вместе.

---

## Этап 3. posts: домен и сценарии

**3.1 `domain/model/Post.java`** — record (Р1-6):
`id, authorId, title, content, PostStatus status, Instant createdAt, Instant updatedAt`.
- `Post update(UUID actorId, String title, String content, Instant now)` — новый `Post` с новыми полями и `updatedAt = now`.
- `Post delete(UUID actorId, Instant now)` — новый `Post` со статусом `DELETED`.
- Оба: `!authorId.equals(actorId)` или `status != PUBLISHED` → `PostNotFoundException(id)` (Р1-7: чужой пост = отсутствующий). Проверка — в приватном `requireEditableBy(actorId)`.
- Без Spring и аннотаций (правило domain в `ApplicationModulesTest`).

**3.2 Команды** (`application/model`):
`CreatePostCommand(UUID actorId, String title, String content)`, `UpdatePostCommand(UUID postId, UUID actorId, String title, String content)`, `DeletePostCommand(UUID postId, UUID actorId)`, `GetUserPostsCommand(UUID authorId, Instant afterCreatedAt, UUID afterId, int limit)`.
Результат страницы — существующие `FeedPageResult`/`FeedCursorResult` **(план: переиспользовать, не дублировать)**.

**3.3 `PostRepository`** — добавить по образцу `FeedRepository`:
```java
FeedPageResponse findPublishedByAuthor(UUID authorId, int limit);
FeedPageResponse findPublishedByAuthorAfter(UUID authorId, FeedCursor after, int limit);
```
Javadoc: порядок `created_at DESC, id DESC`, только `PUBLISHED`. Существующие `updatePost`/`deletePost` и их `WHERE author_id/status` остаются (защита от гонок — Р1-6).

**3.4 `PostApplicationMapper`** — `Post toDomain(PostResponse)`, `PostResult toResult(PostResponse)` (если нет), `UpdatePostRequest toUpdateRequest(Post)`, `DeletePostRequest toDeleteRequest(Post, UUID actorId)` — по потребности use case'ов.

**3.5 Use case'ы** (`@Service`, `@Transactional`, время — из `Clock` бина `shared.config.ClockConfiguration`):
- `CreatePost.create(CreatePostCommand)` → `CreatePostRequest(UUID.randomUUID(), actorId, title, content, PUBLISHED, now, now)` → `PostResult`.
- `UpdatePost.update(UpdatePostCommand)` → `findPublishedById` (нет → `PostNotFoundException`) → `toDomain` → `post.update(actorId, …, now)` → `updatePost(request)`; пусто (пост удалён/изменён между чтением и записью) → `PostNotFoundException` → `PostResult`.
- `DeletePost.delete(DeletePostCommand)` → то же с `post.delete(actorId, now)` → `deletePost(request)`; `false` → `PostNotFoundException`.
- `GetUserPosts.get(GetUserPostsCommand)` — копия логики `GetFeed` (неполный курсор → `IncompleteFeedCursorException`).

**3.6 `PostPersistenceAdapter`** — два запроса по индексу `idx_posts_published_author_created` (уже есть, 008):
```sql
SELECT … FROM posts WHERE author_id = ? AND status = 'PUBLISHED'
ORDER BY created_at DESC, id DESC LIMIT ?
-- After: … AND (created_at, id) < (?, ?) …
```
Сборку страницы (`limit + 1` → `next`) — как в `FeedPersistenceAdapter`; общий код не выносить, если он там приватный **(план)**.

**3.7 Тесты** (минимально, Р: happy-path):
- `PostTest`: автор меняет/удаляет → новые поля/`DELETED`; чужой → `PostNotFoundException`.
- `CreatePostTest`, `UpdatePostTest`, `DeletePostTest`: Mockito на порт, happy-path.
- `PostPersistenceAdapterTest`: `findPublishedByAuthor` — свои посты в порядке, без удалённых и чужих.

---

## Этап 4. posts: web

**4.1 `PostWebMapper`**:
- `CreatePostCommand toCreateCommand(UUID actorId, PostWebRequest request)`; `UpdatePostCommand toUpdateCommand(UUID postId, UUID actorId, PostWebRequest request)`; `DeletePostCommand toDeleteCommand(UUID postId, UUID actorId)`; `GetUserPostsCommand toGetUserPostsCommand(UUID authorId, Integer limit, Instant afterCreatedAt, UUID afterId)`.
- `FeedPageWebResponse`/`FeedCursorWebResponse` → `PostPageWebResponse`/`PostCursorWebResponse`.

**4.2 `PostController`** — поле `CurrentUserProvider currentUser` + use case'ы:
- `createPost(PostWebRequest request)`: `actorId = currentUser.currentUserId()` → `CreatePost` → `ResponseEntity.created(URI.create("/api/v1/posts/" + id)).body(...)` (Р1-10).
- `updatePost(UUID postId, PostWebRequest request)` → `200` + пост.
- `deletePost(UUID postId)` → `ResponseEntity.noContent().build()`.
- `getUserPosts(...)` → `200` + страница; `currentUser` не вызывается (публичный).
- `authorId` из запроса не читается нигде (Р0-4).

**4.3 `FeedController`** — `getFeed(Integer limit, Instant afterCreatedAt, UUID afterId)`; `followerId = currentUser.currentUserId()` (Р3-3). `GetFeed` и `GetFeedCommand` не меняются.

**4.4 `PostExceptionHandler`** — в список `VALIDATION_ERROR` добавить `MethodArgumentNotValidException` (тело не прошло `@Valid`) и `HttpMessageNotReadableException` (тело не читается). `UnauthenticatedException` здесь не ловить — его отвечает `GlobalExceptionHandler` (1.5). Javadoc обновить.

**4.5 Тесты** — `PostControllerTest`: убрать `followerId` из запросов ленты, добавить заголовок `X-User-Id`; новые кейсы: `POST` → `201` + `Location`; `PUT` чужого → `404`; без заголовка → `401`. `PostWebMapperTest` — под новые сигнатуры.

**4.6 `docs/process/Feed.md`** — читатель ленты — текущий пользователь (`CurrentUserProvider`), не параметр.

---

## Этап 5. subscriptions: подписка и отписка

**5.1 Контракт** **(план — имена файлов и тела)**:
- `schemas/subscription.yaml` — вынести инлайн-элемент ответа `subscribes-get.yaml` (`{id, userId}`); класс остаётся `SubscriptionWebResponse` (через `model-names`), код чтения списков не меняется.
- `subscribes-get.yaml` → `subscribes.yaml`: прежний `get` + `post` (`operationId: subscribe`, tag `Subscription`, `security`), тело `{ userId: uuid }` — на кого подписаться (подписчик — из порта, Р0-3). Ответы: `201`/`200` → `subscription.yaml` (Р2-3), `400` (тело, не UUID, подписка на себя), `401`, `500`.
- `subscribe-delete.yaml`: `/api/v1/subscribes/{userId}`, `delete`, `operationId: unsubscribe`, `security`; `204` (всегда, Р2-3), `400`, `401`, `500`.

**5.2** Пути и `components.securitySchemes` — в `openapi.yaml` и `modules/subscriptions.yaml`.

**5.3 `pom.xml`** — execution `subscriptions-requests` (`SubscribeWebRequest` ← инлайн `subscribe_request`), `model-names` `subscription=SubscriptionWebResponse` (инлайн-имя `getSubscriptions_200_response_inner` убрать), `imports` += `SubscribeWebRequest`.

**5.4 Модели** — `SubscribeCommand(UUID followerId, UUID followedId)`, `UnsubscribeCommand(UUID followerId, UUID followedId)`, `SubscribeResult(SubscriptionResult subscription, boolean created)`.

**5.5 Порт и адаптер** — `Optional<SubscriptionResponse> findSubscription(UUID followerId, UUID followedId)` (для `200` с существующей подпиской). Javadoc — только для ответа после `createSubscriptionIfAbsent`, не как проверка перед вставкой.

**5.6 Use case'ы**:
- `Subscribe.subscribe(cmd)`: `followerId.equals(followedId)` → `SubscriptionToSelfException` (класс уже есть) → `createSubscriptionIfAbsent` → есть → `created = true`; пусто → `findSubscription(...).orElseThrow()` → `created = false`.
- `Unsubscribe.unsubscribe(cmd)` → `deleteSubscription`, результат не важен (идемпотентно).

**5.7 Web** — `SubscriptionController`: `subscribe(SubscribeWebRequest)` → `201`/`200` по `created`; `unsubscribe(UUID userId)` → `204`; подписчик — `currentUser.currentUserId()`. `SubscriptionExceptionHandler`: `SubscriptionToSelfException`, `MethodArgumentNotValidException`, `HttpMessageNotReadableException` → `VALIDATION_ERROR`. Мапперы — под новые команды/результат.

**5.8 `SubscribeTest`** — новая подписка → `created = true`; повторная → `false` + существующая.

---

## Этап 6. Сценарный MockMvc-тест

**6.1 `PostFeedScenarioTest`** (корневой пакет — затрагивает posts и subscriptions; конфигурация как у `FriendshipFeedTest`: `@SpringBootTest`, `@AutoConfigureMockMvc`, `@Import(TestcontainersConfiguration.class)`):
1. Алиса: `POST /api/v1/subscribes` `{userId: boris}`, `X-User-Id: alice` → `201`.
2. Борис: `POST /api/v1/posts` `{title, content}`, `X-User-Id: boris` → `201`, запомнить `id`.
3. Алиса: `GET /api/v1/feed`, `X-User-Id: alice` → `posts[*].id` содержит `id`.
4. Алиса: `DELETE /api/v1/subscribes/{boris}` → `204`.
5. Алиса: `GET /api/v1/feed` → `posts[*].id` не содержит `id`.
ID — `UUID.randomUUID()`; `var` в тестах допустим.

**6.2 `FriendshipFeedTest`** — Javadoc «module has no REST API yet» устарел: поправить (переводить создание постов на REST — по желанию).

---

## Этап 7. Ручные тесты
`manual-test-curl.md` (скрипт с OK/FAIL), `manual-test-bruno.md` + коллекция `bruno/41225/`. Вместо токена — заголовок `X-User-Id: {{aliceId}}` / `{{borisId}}` (Р3-1). Делаю по команде после этапа 6.

---

По окончании — напомню про `ApplicationModulesTest` (новый пакет `shared.security`, новые классы в posts/subscriptions); запускаю только по команде.
