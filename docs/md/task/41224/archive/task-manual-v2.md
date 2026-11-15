# 41224 — HTTP-статусы процесса. План для ручной реализации

Режим Ментора: ДА. Эталон — `decisions.md`; при расхождении плана с решением прав `decisions.md`.

**3 этапа, по модулю за этап (Р0-2):** friendships → posts → subscriptions.
После каждого этапа `./mvnw verify` зелёный, модули ещё не переведённые работают по-старому.
Общая часть (схемы, генерация, `ApiErrors`, обработчик 500) делается в этапе 1, удаление старого (`error.yaml`, `ErrorResponse`) — в этапе 3.

Итого: **6 новых**, **6 удаляемых**, **37 изменяемых** файлов.

| Этап | Новые | Удалить | Изменить |
|---|---|---|---|
| 1. friendships + общее | 6 | 2 | 24 (8 path, 3 md, pom, 6 Java, 6 тестов) |
| 2. posts | — | 1 | 8 (enum кодов, 2 path, pom, 3 Java, 1 тест) |
| 3. subscriptions + очистка | — | 3 | 5 (2 path, pom, 2 Java) |

## Где будут файлы

```
docs/api/
├── modules/shared.yaml                      (этап 1, новый) спецификация только для генерации общих моделей
├── responses/internal-error.yaml            (этап 1, новый) общий ответ 500
├── schemas/api-error-code.yaml              (этап 1, новый; этап 2 — + POST_NOT_FOUND)
├── schemas/api-error-response.yaml          (этап 1, новый)
├── schemas/error.yaml                       (этап 3, удалить)
└── paths/*.yaml                             (8 — этап 1, 2 — этап 2, 2 — этап 3)

src/main/java/ru/job4j/socialmediaapi/
├── shared/web/ApiErrors.java                (этап 1, новый)
├── shared/web/ErrorResponse.java            (этап 3, удалить)
├── shared/exception/GlobalExceptionHandler.java  (этап 1)
├── friendships/internal/...                 (этап 1)
├── posts/internal/infrastructure/web/...    (этап 2)
└── subscriptions/internal/infrastructure/web/...  (этап 3)

target/generated-sources/openapi/ru/job4j/socialmediaapi/shared/web/
└── ApiErrorResponse.java, ApiErrorCode.java (генерируются с этапа 1)

src/test/java/ru/job4j/socialmediaapi/
└── shared/exception/UnexpectedErrorE2ETest.java  (этап 1, новый) E2E на 500
```

## Файлы

| № | Файл | Слой | Действие | Решение |
|---|---|---|---|---|
| **Этап 1** | | | | |
| 1.1 | `docs/api/schemas/api-error-code.yaml` | контракт | новый | Р1-2, Р2-4 |
| 1.2 | `docs/api/schemas/api-error-response.yaml` | контракт | новый | Р1-1, Р1-3 |
| 1.3 | `docs/api/responses/internal-error.yaml` | контракт | новый | Р1-10 |
| 1.4 | `docs/api/modules/shared.yaml` | генерация | новый | Р1-4 |
| 1.5 | `pom.xml` | генерация | изменить | Р1-4, Р1-6, Р2-5 |
| 1.6 | `docs/api/paths/` — 8 файлов friendships | контракт | изменить | Р1-1, Р1-5, Р1-7, Р1-8, Р2-2, Р2-3 |
| 1.7 | `docs/api/friendship-offer-{create,accept,reject}.md` | документация | изменить | Р1-1, Р1-7, Р2-3 |
| 1.8 | `shared/web/ApiErrors.java` | shared | новый | Р1-4, Р1-5, Р1-7 |
| 1.9 | `shared/exception/GlobalExceptionHandler.java` | shared | изменить | Р1-10, Р3-1 |
| 1.10 | `CreateFriendshipOfferResult.java` | application | изменить | Р2-1 |
| 1.11 | `FriendshipOfferApplicationMapper.java` | application | изменить | Р2-1 |
| 1.12 | `CreateFriendshipOffer.java` | application | изменить | Р2-1 |
| 1.13 | `FriendshipOfferAlreadyPendingException.java` | domain | удалить | Р2-1 |
| 1.14 | `friendships/.../web/error/ErrorCode.java` | web | удалить | Р1-2 |
| 1.15 | `FriendshipOfferExceptionHandler.java` | web | изменить | Р1-6, Р1-7, Р2-2 |
| 1.16 | `FriendshipOfferController.java` | web | изменить | Р1-6, Р2-1 |
| 1.17 | тесты friendships + shared (6) и `UnexpectedErrorE2ETest` | test | изменить / новый | Р1-1, Р1-10, Р2-1 |
| **Этап 2** | | | | |
| 2.1 | `docs/api/schemas/api-error-code.yaml` | контракт | + `POST_NOT_FOUND` | Р2-4 |
| 2.2 | `docs/api/paths/post-get.yaml`, `feed-get.yaml` | контракт | изменить | Р1-1, Р1-5, Р1-7 |
| 2.3 | `pom.xml` | генерация | изменить | Р1-4 |
| 2.4 | `posts/.../web/error/ErrorCode.java` | web | удалить | Р1-2 |
| 2.5 | `PostExceptionHandler.java`, `PostController.java`, `FeedController.java` | web | изменить | Р1-6, Р1-7 |
| 2.6 | `PostControllerTest.java` | test | изменить | Р1-1 |
| **Этап 3** | | | | |
| 3.1 | `docs/api/paths/followers-get.yaml`, `subscribes-get.yaml` | контракт | изменить | Р1-1, Р1-5, Р1-7, Р2-3 |
| 3.2 | `pom.xml` | генерация | изменить | Р1-4 |
| 3.3 | `subscriptions/.../web/error/ErrorCode.java` | web | удалить | Р1-2 |
| 3.4 | `SubscriptionExceptionHandler.java`, `SubscriptionController.java` | web | изменить | Р1-6, Р1-7 |
| 3.5 | `docs/api/schemas/error.yaml`, `shared/web/ErrorResponse.java` | контракт / shared | удалить | Р1-3, Р1-4 |

---

## Этап 1. Общая часть + friendships

### Контракт и генерация (п.1–3 задания)

**1.1. `docs/api/schemas/api-error-code.yaml`** — контракт. Зачем: закрытый перечень машинных кодов вместо произвольной строки.
`type: string`, `enum` — только коды этого этапа: `VALIDATION_ERROR`, `FRIENDSHIP_OFFER_NOT_FOUND`, `FRIENDSHIP_OFFER_ALREADY_ACCEPTED`, `FRIENDSHIP_OFFER_ALREADY_REJECTED`, `FRIENDSHIP_OFFER_ALREADY_EXISTS`, `INTERNAL_ERROR`.
Новый код сначала добавляется сюда (так в этапе 2 появится `POST_NOT_FOUND`). → Р1-2, Р2-4

**1.2. `docs/api/schemas/api-error-response.yaml`** — контракт. Зачем: одна схема тела ошибки вместо копий в каждом ответе.
`required: [status, detail, code]`; `status` — `integer/int32`, `detail` — `string`, `code` — `$ref: './api-error-code.yaml'`. Без `title` (урок). → Р1-1

**1.3. `docs/api/responses/internal-error.yaml`** — контракт. Зачем: исход 500 есть у каждой операции, описать его один раз.
Объект Response: `description`, `content: application/problem+json`, `schema: $ref '../schemas/api-error-response.yaml'`, `example` `{status: 500, detail: ..., code: INTERNAL_ERROR}`. → Р1-10

**1.4. `docs/api/modules/shared.yaml`** — генерация. Зачем: общие классы генерируются один раз, а не в каждом модуле.
Минимальная спецификация: `openapi: 3.0.3`, `info`, `paths: {}`, `components.schemas` со ссылками на файлы 1.1–1.2. → Р1-4

**1.5. `pom.xml`** — генерация.
- в общих `configOptions` — `<generateGenericResponseEntity>true</generateGenericResponseEntity>` (Р1-6).
  Флаг общий, но posts и subscriptions от этого не сломаются: `ResponseEntity<PostWebResponse>` в контроллере — допустимый ковариантный тип для `ResponseEntity<?>` интерфейса;
- новый execution `shared-api-errors`: `inputSpec` — 1.4, `modelPackage` — `ru.job4j.socialmediaapi.shared.web`, `generateApis=false`, `modelsToGenerate` и `modelNameMappings` (`api-error-response=ApiErrorResponse,api-error-code=ApiErrorCode`) — по образцу `friendships-responses` (Р1-4);
- только friendships: в `openapi.friendships.schemas` `error=ErrorResponse` → `api-error-response=ApiErrorResponse,api-error-code=ApiErrorCode`; в `openapi.friendships.imports` — `ApiErrorResponse=…shared.web.ApiErrorResponse`, `ApiErrorCode=…shared.web.ApiErrorCode`, `ErrorResponse=…` убрать;
- убрать `createFriendshipOffer_200_response` из `openapi.friendships.schemas` и комментарий-обоснование (Р2-5).

**1.6. `docs/api/paths/` — 8 файлов friendships** (`friendship-offers-get`, `friendship-offer-get`, `-create`, `-accept`, `-reject`, `friendships-incoming-get`, `friendships-outgoing-get`, `friends-get`) — контракт.
В каждом ответе 4xx:
- `application/json` → `application/problem+json` (Р1-5);
- `$ref: '../schemas/error.yaml'` → `'../schemas/api-error-response.yaml'`;
- пример → `{status, detail, code}`; все 400 — `VALIDATION_ERROR` (Р1-7);
- `'500': $ref: '../responses/internal-error.yaml'` во все операции.

Отдельно:
- `create`, `incoming`, `outgoing`, `friends-get` — удалить `404 USER_NOT_FOUND` (Р2-3);
- `create`: `'200'` — та же схема, что у `'201'` (вынести в `schemas/` или сослаться), описание — «предложение пары уже ждёт решения, возвращается существующее» (Р1-8, Р2-5); в `'409'` — пример `FRIENDSHIP_OFFER_ALREADY_EXISTS` (Р2-2);
- `accept`/`reject`: `409` уже есть — только `$ref` и формат (п.1 задания: повтор — это `409`, а не `204`).

`error.yaml` пока не удалять — на него ссылаются posts и subscriptions.

**1.7. `docs/api/friendship-offer-{create,accept,reject}.md`** — документация. Таблицы кодов: `message` → `detail`, `INVALID_*` → `VALIDATION_ERROR`, убрать `USER_NOT_FOUND` и `FRIENDSHIP_OFFER_ALREADY_PENDING`, у create — `200` с телом предложения. → Р1-1, Р1-7, Р2-1, Р2-3

**Проверка генерации:** `./mvnw clean generate-sources` (Р1-12):
- в `target/generated-sources/openapi/ru/job4j/socialmediaapi/shared/web/` есть `ApiErrorResponse` и `ApiErrorCode`;
- в пакетах friendships **нет** своих копий этих классов (если есть — не хватает `schemaMappings`/`importMappings`);
- методы `FriendshipOfferApi` и других `*Api` возвращают `ResponseEntity<?>`.

### Java: общая часть (п.6 задания)

**1.8. `shared/web/ApiErrors.java`** — shared. Зачем: тело и `Content-Type` собираются в одном месте, обработчики модулей остаются однострочными.
`final`-класс со статическими методами:
- `response(HttpStatus status, ApiErrorCode code, String detail)` → `ResponseEntity<ApiErrorResponse>` с `contentType(MediaType.APPLICATION_PROBLEM_JSON)` и телом `new ApiErrorResponse().status(status.value()).detail(detail).code(code)`;
- `invalidRequest()` → `400 VALIDATION_ERROR` с общим текстом «Некорректное тело или параметры запроса» (один текст для всех модулей, Р1-7).
Это решение плана (вместо модульных `ErrorCode`); если сделаете иначе — запишем в `decisions.md`. → Р1-4, Р1-5, Р1-7

**1.9. `shared/exception/GlobalExceptionHandler.java`** — shared.
- `extends ResponseEntityExceptionHandler` (Р3-1 — **ждёт ответа в fix.md**);
- `handleUnexpected(Exception)` → `LOG.error(...)` + `ApiErrors.response(INTERNAL_SERVER_ERROR, INTERNAL_ERROR, «Внутренняя ошибка сервера»)`;
- `handleDataIntegrityViolation` оставить, перевести на `ApiErrors`;
- строковую константу `INTERNAL_ERROR` удалить: код теперь `ApiErrorCode.INTERNAL_ERROR`.
В `detail` не возвращать `exception.getMessage()` — там могут быть SQL и имена классов.
Обработчик общий, поэтому с этого этапа 500 во всех модулях отдаётся в новом формате — это ожидаемо. → Р1-10

### Java: friendships (п.4–5 задания)

**1.10. `CreateFriendshipOfferResult.java`** — application. Добавить `boolean created` (`true` — создано, `false` — возвращено существующее `PENDING`). → Р2-1

**1.11. `FriendshipOfferApplicationMapper.java`** — application.
- `toResult(CreateFriendshipOfferResponse)` → `created = true` (`@Mapping(target = "created", constant = "true")`);
- новый метод из `FindFriendshipOfferResponse` в `CreateFriendshipOfferResult` с `created = false`, например `toExistingResult` — `toResult(FindFriendshipOfferResponse)` уже занят. → Р2-1

**1.12. `CreateFriendshipOffer.java`** — application.
При `FriendshipOfferAlreadyExistsException` читается пара (`findByPair`):
- `PENDING` → **вернуть** `toExistingResult(existing)`, подписку **не** создавать (ничего не изменилось);
- `ACCEPTED` / `REJECTED` → бросить, как сейчас;
- не прочиталось → бросить исходное `FriendshipOfferAlreadyExistsException`.
`alreadyExistsError` теперь и возвращает результат — переделать структуру метода. Обновить Javadoc (`@throws` без `AlreadyPending`, смысл `created`).
Подсказка: insert откатывается до savepoint, поэтому нормальный возврат из `@Transactional` ничего лишнего не закоммитит — проверьте тестом «данные не меняются». → Р2-1

**1.13. `FriendshipOfferAlreadyPendingException.java`** — удалить. → Р2-1

**1.14. `friendships/.../web/error/ErrorCode.java`** — удалить, вместо него `ApiErrorCode` + `ApiErrors`. → Р1-2

**1.15. `FriendshipOfferExceptionHandler.java`** — web.
- возвращаемый тип — `ResponseEntity<ApiErrorResponse>`;
- `handleInvalidRequest` → `ApiErrors.invalidRequest()`;
- `handleAlreadyPending` — удалить;
- `handleNotFound` / `handleAlreadyAccepted` / `handleAlreadyRejected` → `ApiErrors.response(...)` с прежними статусами и текстами;
- `handleAlreadyExists` → `409 FRIENDSHIP_OFFER_ALREADY_EXISTS` + `LOG.warn` (Р2-2).
→ Р1-6, Р1-7, Р2-2

**1.16. `FriendshipOfferController.java`** — web.
- все методы — `ResponseEntity<?>`;
- create: `result.created() ? 201 : 200`, тело в обоих случаях — `mapper.toResponse(result)`;
- Javadoc: убрать `FRIENDSHIP_OFFER_ALREADY_PENDING`.
Лишнее поле `created` MapStruct в `FriendshipOfferWebMapper` по умолчанию игнорирует; если будет предупреждение — `@Mapping(..., ignore = true)`. → Р1-6, Р2-1

### Тесты этапа 1 (п.7 задания)

**1.17.**

| Тест | Что поменять |
|---|---|
| `GlobalExceptionHandlerTest` | тело `ApiErrorResponse`; + кейс `handleUnexpected` → 500 `INTERNAL_ERROR` |
| `CreateFriendshipOfferTest` | повтор и встречное при `PENDING` → результат с `created=false`, без исключения, подписка не вызывается |
| `CreateFriendshipOfferConcurrencyTest` | вместо `AlreadyPending`: один `created=true`, остальные `created=false` |
| `FriendshipOfferExceptionHandlerTest` | удалить кейс pending; `AlreadyExists` → 409 `FRIENDSHIP_OFFER_ALREADY_EXISTS` |
| `FriendshipOfferControllerTest` (MockMvc) | повтор при `PENDING` → 200 с телом предложения; `$.message` → `$.detail`, `+ $.status`; `Content-Type` problem+json |
| `FriendshipOfferE2ETest` | `ErrorResponse` → `ApiErrorResponse`; `INVALID_*` → `VALIDATION_ERROR`; pending → 200 с существующим предложением (у встречной — направление первой). 404 и 409 уже есть |
| `UnexpectedErrorE2ETest` (новый, `shared/exception`) | `@E2ETest` + `@MockitoBean GetFriendshipOffer`, который бросает `RuntimeException` → `GET /api/v1/friendship-offer/{id}` → 500 `INTERNAL_ERROR`, текста исключения в теле нет |

`@MockitoBean` создаёт отдельный Spring-контекст — поэтому 500 вынесен в свой класс.

**Конец этапа 1:** `./mvnw verify` зелёный (posts и subscriptions пока отвечают `{code, message}`).

---

## Этап 2. posts

**2.1. `docs/api/schemas/api-error-code.yaml`** — добавить `POST_NOT_FOUND`. → Р2-4

**2.2. `docs/api/paths/post-get.yaml`, `feed-get.yaml`** — как в 1.6: `problem+json`, `$ref` на `api-error-response.yaml`, примеры `{status, detail, code}`, 400 → `VALIDATION_ERROR`, + `'500'`. → Р1-1, Р1-5, Р1-7

**2.3. `pom.xml`** — `openapi.posts.schemas` и `openapi.posts.imports` — как для friendships в 1.5. → Р1-4

**2.4. `posts/.../web/error/ErrorCode.java`** — удалить. → Р1-2

**2.5. `PostExceptionHandler.java`** — 400 → `ApiErrors.invalidRequest()`, `PostNotFoundException` → `ApiErrors.response(NOT_FOUND, POST_NOT_FOUND, ...)`.
**`PostController.java`, `FeedController.java`** — `ResponseEntity<?>`. → Р1-6, Р1-7

**2.6. `PostControllerTest.java`** — формат тела (`detail`, `status`), код 400 → `VALIDATION_ERROR`. → Р1-1

**Конец этапа 2:** `./mvnw verify` зелёный.

---

## Этап 3. subscriptions + очистка

**3.1. `docs/api/paths/followers-get.yaml`, `subscribes-get.yaml`** — как в 1.6; удалить `404 USER_NOT_FOUND` (Р2-3); 400 → `VALIDATION_ERROR`. → Р1-1, Р1-5, Р1-7, Р2-3

**3.2. `pom.xml`** — `openapi.subscriptions.schemas` и `openapi.subscriptions.imports` — как в 1.5. → Р1-4

**3.3. `subscriptions/.../web/error/ErrorCode.java`** — удалить (`INVALID_USER_ID` → `VALIDATION_ERROR`). → Р1-2, Р1-7

**3.4. `SubscriptionExceptionHandler.java`** — 400 → `ApiErrors.invalidRequest()`.
**`SubscriptionController.java`** — `ResponseEntity<?>`. → Р1-6
Web-тестов у subscriptions нет, новые не пишем — 400 проверяется ручными тестами.

**3.5. Очистка:**
- `docs/api/schemas/error.yaml` — удалить, если `grep -r "error.yaml" docs/api` пуст (Р1-3);
- `shared/web/ErrorResponse.java` — удалить, если `grep -r "web.ErrorResponse" src` пуст (Р1-4).

**Конец этапа 3:** `./mvnw verify` зелёный.

---

## После этапов

- `ApplicationModulesTest` — по команде (затронуты `shared` и все модули).
- П.5 задания: `grep -rn "DataAccessException\|DataIntegrityViolation" src/main/java` — вне `persistence/adapter` и `GlobalExceptionHandler` их быть не должно.
- Ручные тесты — `manual-test-curl.md`, `manual-test-bruno.md` и коллекция `bruno/41224/`: 201/200 create, 404/409 accept, 400 `VALIDATION_ERROR` в каждом модуле, `Content-Type: application/problem+json`. Готовлю я по команде.
