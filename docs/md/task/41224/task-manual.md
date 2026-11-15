# 41224 — HTTP-статусы процесса. План для ручной реализации

Режим Ментора: ДА. Эталон — `decisions.md`; при расхождении плана с решением прав `decisions.md`.

Реализация — **3 последовательных этапа, по модулю за этап** (Р0-2, Р4-4): friendships → posts → subscriptions.
Каждый этап самостоятелен: после него `./mvnw verify` зелёный, ещё не переведённые модули работают по-старому.
Общая часть (схема ошибки, генерация, `ErrorCode` + `CommonErrorCode`, обработчик 500) — в этапе 1, удаление старого (`error.yaml`, `ErrorResponse`) — в этапе 3.

Ключевые решения: тело ошибки `{status, detail, code}` (Р1-1), `application/problem+json` (Р1-5), коды — по модулям (`<Module>ErrorCode`, Р0-3), в контракте `code` — строка (Р0-4), все 400 — `VALIDATION_ERROR` (Р1-7), повтор create при `PENDING` — `200` + `FRIENDSHIP_OFFER_ALREADY_PENDING` как было (Р4-1).

| Этап | Новые | Удалить | Изменить |
|---|---|---|---|
| 1. friendships + общее | 6 | — | 21 (8 path, 3 md, pom, 4 Java, 5 тестов) |
| 2. posts | — | — | 8 (2 path, pom, 4 Java, 1 тест) |
| 3. subscriptions + очистка | — | 3 | 5 (2 path, pom, 2 Java) |
| **Итого** | **6** | **3** | **34** (из них 2 переименования) |

---

## Этап 1. Общая часть + friendships

### Где будут файлы

```
docs/api/
├── modules/shared.yaml                      (новый) спецификация только для генерации общей модели
├── responses/internal-error.yaml            (новый) общий ответ 500
├── schemas/api-error-response.yaml          (новый) тело ошибки, code — string
├── paths/friendship-offer*.yaml, friendships-*.yaml, friends-get.yaml   (8, изменить)
└── friendship-offer-{create,accept,reject}.md                         (3, изменить)

pom.xml                                       (изменить)

src/main/java/ru/job4j/socialmediaapi/
├── shared/web/ErrorCode.java                (новый) интерфейс кода ошибки
├── shared/web/CommonErrorCode.java          (новый) VALIDATION_ERROR, INTERNAL_ERROR
├── shared/exception/GlobalExceptionHandler.java  (изменить)
└── friendships/internal/infrastructure/web/
    ├── error/ErrorCode.java → error/FriendshipOfferErrorCode.java  (переименовать)
    ├── FriendshipOfferExceptionHandler.java (изменить)
    └── FriendshipOfferController.java       (изменить)

target/generated-sources/openapi/ru/job4j/socialmediaapi/shared/web/
└── ApiErrorResponse.java                    (генерируется)

src/test/java/ru/job4j/socialmediaapi/
├── shared/exception/GlobalExceptionHandlerTest.java   (изменить)
├── shared/exception/UnexpectedErrorE2ETest.java       (новый) E2E на 500
└── friendships/... — FriendshipOfferExceptionHandlerTest, FriendshipOfferControllerTest,
    FriendshipOfferGetControllerTest, FriendshipOfferE2ETest  (изменить)
```

### Файлы

| № | Файл | Слой | Действие | Решение |
|---|---|---|---|---|
| 1.1 | `docs/api/schemas/api-error-response.yaml` | контракт | новый | Р1-1, Р0-4 |
| 1.2 | `docs/api/responses/internal-error.yaml` | контракт | новый | Р1-10 |
| 1.3 | `docs/api/modules/shared.yaml` | генерация | новый | Р0-5 |
| 1.4 | `pom.xml` | генерация | изменить | Р0-5, Р1-6, Р4-3 |
| 1.5 | `docs/api/paths/` — 8 файлов friendships | контракт | изменить | Р1-1, Р1-5, Р1-7, Р4-1, Р2-2, Р2-3 |
| 1.6 | `docs/api/friendship-offer-{create,accept,reject}.md` | документация | изменить | Р1-1, Р1-7, Р2-3 |
| 1.7 | `shared/web/ErrorCode.java`, `shared/web/CommonErrorCode.java` | shared | новые | Р0-3, Р1-5, Р1-7 |
| 1.8 | `shared/exception/GlobalExceptionHandler.java` | shared | изменить | Р1-10, Р3-1 |
| 1.9 | `web/error/ErrorCode.java` → `FriendshipOfferErrorCode.java` | web | переименовать, изменить | Р0-3, Р4-2 |
| 1.10 | `FriendshipOfferExceptionHandler.java` | web | изменить | Р0-3, Р1-6, Р1-7, Р2-2, Р4-1 |
| 1.11 | `FriendshipOfferController.java` | web | изменить | Р1-6 |
| 1.12 | тесты (5) и `UnexpectedErrorE2ETest` | test | изменить / новый | Р1-1, Р1-10, Р4-1 |

Application- и domain-слой friendships не меняются: `CreateFriendshipOffer` по-прежнему бросает `FriendshipOfferAlreadyPendingException` (Р4-1).

### Контракт и генерация (п.1–3 задания)

**1.1. `docs/api/schemas/api-error-response.yaml`** — контракт. Зачем: одна схема тела ошибки вместо копий в каждом ответе.
`required: [status, detail, code]`; `status` — `integer/int32`, `detail` — `string`, `code` — `string` с `description` «машинный код ошибки; возможные значения — в примерах ответов операции» и `example: VALIDATION_ERROR`. Без `enum`, без `title`. → Р1-1, Р0-4

**1.2. `docs/api/responses/internal-error.yaml`** — контракт. Зачем: исход 500 есть у каждой операции, описать его один раз.
Объект Response: `description`, `content: application/problem+json`, `schema: $ref '../schemas/api-error-response.yaml'`, `example` `{status: 500, detail: ..., code: INTERNAL_ERROR}`. → Р1-10

**1.3. `docs/api/modules/shared.yaml`** — генерация. Зачем: общий класс генерируется один раз, а не в каждом модуле.
Минимальная спецификация: `openapi: 3.0.3`, `info`, `paths: {}`, `components.schemas` со ссылкой на файл 1.1. → Р0-5

**1.4. `pom.xml`** — генерация.
- в общих `configOptions` — `<generateGenericResponseEntity>true</generateGenericResponseEntity>` (Р1-6).
  Флаг общий, но posts и subscriptions не сломаются: `ResponseEntity<PostWebResponse>` в контроллере — допустимый ковариантный тип для `ResponseEntity<?>` интерфейса;
- новый execution `shared-api-errors`: `inputSpec` — 1.3, `modelPackage` — `ru.job4j.socialmediaapi.shared.web`, `generateApis=false`, `modelsToGenerate` и `modelNameMappings` (`api-error-response=ApiErrorResponse`) — по образцу `friendships-responses` (Р0-5);
- только friendships: в `openapi.friendships.schemas` `error=ErrorResponse` → `api-error-response=ApiErrorResponse`; в `openapi.friendships.imports` — `ApiErrorResponse=…shared.web.ApiErrorResponse`, `ErrorResponse=…` убрать;
- убрать `createFriendshipOffer_200_response` из `openapi.friendships.schemas` и комментарий-обоснование (Р4-3).

**1.5. `docs/api/paths/` — 8 файлов friendships** (`friendship-offers-get`, `friendship-offer-get`, `-create`, `-accept`, `-reject`, `friendships-incoming-get`, `friendships-outgoing-get`, `friends-get`) — контракт.
В каждом ответе 4xx:
- `application/json` → `application/problem+json` (Р1-5);
- `$ref: '../schemas/error.yaml'` → `'../schemas/api-error-response.yaml'`;
- пример → `{status, detail, code}`; все 400 — `VALIDATION_ERROR` (Р1-7);
- `'500': $ref: '../responses/internal-error.yaml'` во все операции.

Отдельно:
- `create`, `incoming`, `outgoing`, `friends-get` — удалить `404 USER_NOT_FOUND` (Р2-3);
- `create`: `'200'` — `application/problem+json`, `$ref` на `api-error-response.yaml`, пример `{status: 200, detail: «Предложение дружбы уже отправлено», code: FRIENDSHIP_OFFER_ALREADY_PENDING}` (Р4-1); `'201'` — как сейчас; в `'409'` — пример `FRIENDSHIP_OFFER_ALREADY_EXISTS` (Р2-2);
- `accept`/`reject`: `409` уже есть — только `$ref` и формат (п.1 задания: повтор — это `409`, а не `204`).

`error.yaml` пока не удалять — на него ссылаются posts и subscriptions.

**1.6. `docs/api/friendship-offer-{create,accept,reject}.md`** — документация. Таблицы кодов: `message` → `detail`, + `status`, `INVALID_*` → `VALIDATION_ERROR`, убрать `USER_NOT_FOUND`; у create — `200 FRIENDSHIP_OFFER_ALREADY_PENDING` остаётся, `409` → `FRIENDSHIP_OFFER_ALREADY_EXISTS`. → Р1-1, Р1-7, Р2-2, Р2-3, Р4-1

**Проверка генерации:** `./mvnw clean generate-sources` (Р1-12):
- в `target/generated-sources/openapi/ru/job4j/socialmediaapi/shared/web/` есть `ApiErrorResponse`, поле `code` — `String`;
- в пакетах friendships **нет** своей копии этого класса (если есть — не хватает `schemaMappings`/`importMappings`);
- методы `FriendshipOfferApi` и других `*Api` возвращают `ResponseEntity<?>`.

### Java: общая часть (п.6 задания)

**1.7. `shared/web/ErrorCode.java` и `shared/web/CommonErrorCode.java`** — shared. Зачем: коды живут в своих модулях, а тело и `Content-Type` собираются в одном месте; обработчики остаются однострочными (`XxxErrorCode.Y.response()`, как сейчас).
- `ErrorCode` — интерфейс: `String name()` (реализует сам enum), `HttpStatus status()`, `String detail()` и
  `default ResponseEntity<ApiErrorResponse> response()` — `ResponseEntity.status(status()).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(new ApiErrorResponse().status(status().value()).detail(detail()).code(name()))`;
- `CommonErrorCode implements ErrorCode` — `VALIDATION_ERROR(BAD_REQUEST, «Некорректное тело или параметры запроса»)` (один текст для всех модулей, Р1-7) и `INTERNAL_ERROR(INTERNAL_SERVER_ERROR, «Внутренняя ошибка сервера»)`.
Поля `status`/`detail` и конструктор — как в нынешних модульных `ErrorCode`. → Р0-3, Р1-5, Р1-7

**1.8. `shared/exception/GlobalExceptionHandler.java`** — shared.
- `extends ResponseEntityExceptionHandler` — ошибки Spring MVC (неизвестный URL, неверный метод, `Content-Type`/`Accept`) получают свой статус, а не 500 (Р3-1);
- `handleUnexpected(Exception)` → `LOG.error(...)` + `CommonErrorCode.INTERNAL_ERROR.response()`;
- `handleDataIntegrityViolation` оставить, перевести на `CommonErrorCode.INTERNAL_ERROR.response()`;
- строковую константу `INTERNAL_ERROR` удалить.
В `detail` не возвращать `exception.getMessage()` — там могут быть SQL и имена классов.
Обработчик общий, поэтому с этого этапа 500 во всех модулях отдаётся в новом формате — это ожидаемо. → Р1-10, Р3-1

### Java: friendships (п.4–5 задания)

**1.9. `web/error/ErrorCode.java` → `FriendshipOfferErrorCode.java`** — web. Переименовать (IDEA → Refactor → Rename), затем:
- `implements ErrorCode` (`shared.web`); `message` → `detail`; метод `response()` удалить — он теперь default в интерфейсе;
- удалить `INVALID_FRIENDSHIP_OFFER_REQUEST` (→ `CommonErrorCode.VALIDATION_ERROR`);
- `FRIENDSHIP_OFFER_ALREADY_PENDING(OK, …)` оставить вместе с Javadoc-обоснованием (Р4-1);
- добавить `FRIENDSHIP_OFFER_ALREADY_EXISTS(CONFLICT, «Предложение дружбы для этой пары уже существует»)` (Р2-2);
- Javadoc класса: `INTERNAL_ERROR` и `VALIDATION_ERROR` — в `CommonErrorCode`.
→ Р0-3, Р4-2

**1.10. `FriendshipOfferExceptionHandler.java`** — web.
- возвращаемый тип — `ResponseEntity<ApiErrorResponse>`;
- `handleInvalidRequest` → `CommonErrorCode.VALIDATION_ERROR.response()`;
- `handleAlreadyPending`, `handleNotFound`, `handleAlreadyAccepted`, `handleAlreadyRejected` → `FriendshipOfferErrorCode.X.response()`;
- `handleAlreadyExists` → `FriendshipOfferErrorCode.FRIENDSHIP_OFFER_ALREADY_EXISTS.response()` + `LOG.warn`; поправить Javadoc (было «как для pending, 200»).
→ Р0-3, Р1-6, Р1-7, Р2-2, Р4-1

**1.11. `FriendshipOfferController.java`** — web. Все методы — `ResponseEntity<?>` (сигнатуры `*Api` после 1.4); логика не меняется, create — `201`. → Р1-6

### Тесты этапа 1 (п.7 задания)

**1.12.**

| Тест | Что поменять |
|---|---|
| `GlobalExceptionHandlerTest` | тело `ApiErrorResponse`; + кейс `handleUnexpected` → 500 `INTERNAL_ERROR` |
| `FriendshipOfferExceptionHandlerTest` | `ErrorCode` → `FriendshipOfferErrorCode` / `CommonErrorCode`; pending → 200 `FRIENDSHIP_OFFER_ALREADY_PENDING`; `AlreadyExists` → 409 `FRIENDSHIP_OFFER_ALREADY_EXISTS` |
| `FriendshipOfferControllerTest`, `FriendshipOfferGetControllerTest` (MockMvc) | `$.message` → `$.detail`, + `$.status`; `INVALID_*` → `VALIDATION_ERROR`; `Content-Type` problem+json |
| `FriendshipOfferE2ETest` | `ErrorResponse` → `ApiErrorResponse`; `INVALID_*` → `VALIDATION_ERROR`; pending → 200 с `code = FRIENDSHIP_OFFER_ALREADY_PENDING`, `status = 200`. 404 и 409 уже есть |
| `UnexpectedErrorE2ETest` (новый, `shared/exception`) | `@E2ETest` + `@MockitoBean GetFriendshipOffer`, который бросает `RuntimeException` → `GET /api/v1/friendship-offer/{id}` → 500 `INTERNAL_ERROR`, текста исключения в теле нет |

`@MockitoBean` создаёт отдельный Spring-контекст — поэтому 500 вынесен в свой класс.

**Итог этапа 1:** `./mvnw verify` зелёный; friendships и 500 — в новом формате, posts и subscriptions пока отвечают `{code, message}`.

---

## Этап 2. posts

### Где будут файлы

```
docs/api/paths/post-get.yaml, feed-get.yaml   (изменить)
pom.xml                                       (изменить)

src/main/java/ru/job4j/socialmediaapi/posts/internal/infrastructure/web/
├── error/ErrorCode.java → error/PostErrorCode.java  (переименовать)
├── PostExceptionHandler.java                (изменить)
├── PostController.java                      (изменить)
└── FeedController.java                      (изменить)

src/test/java/.../posts/.../PostControllerTest.java   (изменить)
```

### Файлы

| № | Файл | Слой | Действие | Решение |
|---|---|---|---|---|
| 2.1 | `docs/api/paths/post-get.yaml`, `feed-get.yaml` | контракт | изменить | Р1-1, Р1-5, Р1-7 |
| 2.2 | `pom.xml` | генерация | изменить | Р0-5 |
| 2.3 | `web/error/ErrorCode.java` → `PostErrorCode.java` | web | переименовать, изменить | Р0-3, Р4-2 |
| 2.4 | `PostExceptionHandler.java`, `PostController.java`, `FeedController.java` | web | изменить | Р0-3, Р1-6, Р1-7 |
| 2.5 | `PostControllerTest.java` | test | изменить | Р1-1, Р1-7 |

**2.1. `post-get.yaml`, `feed-get.yaml`** — как в 1.5: `problem+json`, `$ref` на `api-error-response.yaml`, примеры `{status, detail, code}`, 400 → `VALIDATION_ERROR`, + `'500'`. → Р1-1, Р1-5, Р1-7

**2.2. `pom.xml`** — `openapi.posts.schemas` и `openapi.posts.imports` — как для friendships в 1.4. → Р0-5

**2.3. `ErrorCode.java` → `PostErrorCode.java`** — как в 1.9: `implements ErrorCode`, `message` → `detail`, `response()` удалить, `INVALID_POST_REQUEST` удалить (→ `CommonErrorCode.VALIDATION_ERROR`), остаётся `POST_NOT_FOUND`. → Р0-3, Р4-2

**2.4. `PostExceptionHandler.java`** — тип `ResponseEntity<ApiErrorResponse>`; 400 → `CommonErrorCode.VALIDATION_ERROR.response()`, `PostNotFoundException` → `PostErrorCode.POST_NOT_FOUND.response()`.
**`PostController.java`, `FeedController.java`** — `ResponseEntity<?>`. → Р1-6, Р1-7

**2.5. `PostControllerTest.java`** — формат тела (`detail`, `status`), код 400 → `VALIDATION_ERROR`, `Content-Type` problem+json. → Р1-1

**Итог этапа 2:** `./mvnw verify` зелёный; posts — в новом формате.

---

## Этап 3. subscriptions + очистка

### Где будут файлы

```
docs/api/paths/followers-get.yaml, subscribes-get.yaml   (изменить)
docs/api/schemas/error.yaml                               (удалить)
pom.xml                                                   (изменить)

src/main/java/ru/job4j/socialmediaapi/
├── subscriptions/internal/infrastructure/web/
│   ├── error/ErrorCode.java                 (удалить)
│   ├── SubscriptionExceptionHandler.java    (изменить)
│   └── SubscriptionController.java          (изменить)
└── shared/web/ErrorResponse.java            (удалить)
```

### Файлы

| № | Файл | Слой | Действие | Решение |
|---|---|---|---|---|
| 3.1 | `docs/api/paths/followers-get.yaml`, `subscribes-get.yaml` | контракт | изменить | Р1-1, Р1-5, Р1-7, Р2-3 |
| 3.2 | `pom.xml` | генерация | изменить | Р0-5 |
| 3.3 | `subscriptions/.../web/error/ErrorCode.java` | web | удалить | Р4-2, Р1-7 |
| 3.4 | `SubscriptionExceptionHandler.java`, `SubscriptionController.java` | web | изменить | Р1-6, Р1-7 |
| 3.5 | `docs/api/schemas/error.yaml`, `shared/web/ErrorResponse.java` | контракт / shared | удалить | Р0-4, Р0-5 |

**3.1. `followers-get.yaml`, `subscribes-get.yaml`** — как в 1.5; удалить `404 USER_NOT_FOUND` (Р2-3); 400 → `VALIDATION_ERROR`. → Р1-1, Р1-5, Р1-7, Р2-3

**3.2. `pom.xml`** — `openapi.subscriptions.schemas` и `openapi.subscriptions.imports` — как в 1.4. → Р0-5

**3.3. `ErrorCode.java`** — удалить: единственный код `INVALID_USER_ID` → `CommonErrorCode.VALIDATION_ERROR`; `SubscriptionErrorCode` не создаём, пока нет своих кодов. → Р4-2, Р1-7

**3.4. `SubscriptionExceptionHandler.java`** — тип `ResponseEntity<ApiErrorResponse>`; 400 → `CommonErrorCode.VALIDATION_ERROR.response()`.
**`SubscriptionController.java`** — `ResponseEntity<?>`. → Р1-6
Web-тестов у subscriptions нет, новые не пишем — 400 проверяется ручными тестами.

**3.5. Очистка:**
- `docs/api/schemas/error.yaml` — удалить, если `grep -r "schemas/error.yaml" docs/api` пуст (Р0-4);
- `shared/web/ErrorResponse.java` — удалить, если `grep -r "web.ErrorResponse" src` пуст (Р0-5).

**Итог этапа 3:** `./mvnw verify` зелёный; все модули в новом формате, старые схема и класс удалены.

---

## После этапов

- `ApplicationModulesTest` — по команде (затронуты `shared` и все модули).
- П.5 задания: `grep -rn "DataAccessException\|DataIntegrityViolation" src/main/java` — вне `persistence/adapter` и `GlobalExceptionHandler` их быть не должно.
- Ручные тесты — `manual-test-curl.md`, `manual-test-bruno.md` и коллекция `bruno/41224/`: 201 create, 200 `FRIENDSHIP_OFFER_ALREADY_PENDING` на повтор, 404/409 accept, 400 `VALIDATION_ERROR` в каждом модуле, `Content-Type: application/problem+json`. Готовлю я по команде.
