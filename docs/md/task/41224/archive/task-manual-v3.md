# 41224 — HTTP-статусы процесса. План для ручной реализации

Режим Ментора: ДА. Эталон — `decisions.md`; при расхождении плана с решением прав `decisions.md`.

**3 этапа, по модулю за этап (Р0-2):** friendships → posts → subscriptions.
После каждого этапа `./mvnw verify` зелёный, модули ещё не переведённые работают по-старому.
Общая часть (схема ошибки, генерация, `ErrorCode` + `CommonErrorCode`, обработчик 500) делается в этапе 1, удаление старого (`error.yaml`, `ErrorResponse`) — в этапе 3.

Коды ошибок — по модулям (Р0-3): интерфейс `shared.web.ErrorCode`, общий `CommonErrorCode`, модульные `FriendshipOfferErrorCode` и `PostErrorCode`; в контракте `code` — строка (Р0-4).

Итого: **6 новых**, **4 удаляемых**, **38 изменяемых** (из них 2 переименования) файлов.

| Этап | Новые | Удалить | Изменить |
|---|---|---|---|
| 1. friendships + общее | 6 | 1 | 25 (8 path, 3 md, pom, 7 Java, 6 тестов) |
| 2. posts | — | — | 8 (2 path, pom, 4 Java, 1 тест) |
| 3. subscriptions + очистка | — | 3 | 5 (2 path, pom, 2 Java) |

## Где будут файлы

```
docs/api/
├── modules/shared.yaml                      (этап 1, новый) спецификация только для генерации общих моделей
├── responses/internal-error.yaml            (этап 1, новый) общий ответ 500
├── schemas/api-error-response.yaml          (этап 1, новый; code — string)
├── schemas/error.yaml                       (этап 3, удалить)
└── paths/*.yaml                             (8 — этап 1, 2 — этап 2, 2 — этап 3)

src/main/java/ru/job4j/socialmediaapi/
├── shared/web/ErrorCode.java                (этап 1, новый) интерфейс кода ошибки
├── shared/web/CommonErrorCode.java          (этап 1, новый) VALIDATION_ERROR, INTERNAL_ERROR
├── shared/web/ErrorResponse.java            (этап 3, удалить)
├── shared/exception/GlobalExceptionHandler.java  (этап 1)
├── friendships/internal/...                 (этап 1; web/error/ErrorCode → FriendshipOfferErrorCode)
├── posts/internal/infrastructure/web/...    (этап 2; web/error/ErrorCode → PostErrorCode)
└── subscriptions/internal/infrastructure/web/...  (этап 3)

target/generated-sources/openapi/ru/job4j/socialmediaapi/shared/web/
└── ApiErrorResponse.java                    (генерируется с этапа 1)

src/test/java/ru/job4j/socialmediaapi/
└── shared/exception/UnexpectedErrorE2ETest.java  (этап 1, новый) E2E на 500
```

## Файлы

| № | Файл | Слой | Действие | Решение |
|---|---|---|---|---|
| **Этап 1** | | | | |
| 1.1 | — (пункт снят: `api-error-code.yaml` не создаётся) | — | — | Р0-4 |
| 1.2 | `docs/api/schemas/api-error-response.yaml` | контракт | новый | Р1-1, Р0-4 |
| 1.3 | `docs/api/responses/internal-error.yaml` | контракт | новый | Р1-10 |
| 1.4 | `docs/api/modules/shared.yaml` | генерация | новый | Р0-5 |
| 1.5 | `pom.xml` | генерация | изменить | Р0-5, Р1-6, Р2-5 |
| 1.6 | `docs/api/paths/` — 8 файлов friendships | контракт | изменить | Р1-1, Р1-5, Р1-7, Р1-8, Р2-2, Р2-3 |
| 1.7 | `docs/api/friendship-offer-{create,accept,reject}.md` | документация | изменить | Р1-1, Р1-7, Р2-3 |
| 1.8 | `shared/web/ErrorCode.java`, `shared/web/CommonErrorCode.java` | shared | новые | Р0-3, Р1-5, Р1-7 |
| 1.9 | `shared/exception/GlobalExceptionHandler.java` | shared | изменить | Р1-10, Р3-1 |
| 1.10 | `CreateFriendshipOfferResult.java` | application | изменить | Р2-1 |
| 1.11 | `FriendshipOfferApplicationMapper.java` | application | изменить | Р2-1 |
| 1.12 | `CreateFriendshipOffer.java` | application | изменить | Р2-1 |
| 1.13 | `FriendshipOfferAlreadyPendingException.java` | domain | удалить | Р2-1 |
| 1.14 | `friendships/.../web/error/ErrorCode.java` → `FriendshipOfferErrorCode.java` | web | переименовать, изменить | Р0-3, Р0-6 |
| 1.15 | `FriendshipOfferExceptionHandler.java` | web | изменить | Р0-3, Р1-6, Р1-7, Р2-2 |
| 1.16 | `FriendshipOfferController.java` | web | изменить | Р1-6, Р2-1 |
| 1.17 | тесты friendships + shared (6) и `UnexpectedErrorE2ETest` | test | изменить / новый | Р1-1, Р1-10, Р2-1 |
| **Этап 2** | | | | |
| 2.1 | — (пункт снят: код добавляется в `PostErrorCode`, см. 2.4) | — | — | Р0-4 |
| 2.2 | `docs/api/paths/post-get.yaml`, `feed-get.yaml` | контракт | изменить | Р1-1, Р1-5, Р1-7 |
| 2.3 | `pom.xml` | генерация | изменить | Р0-5 |
| 2.4 | `posts/.../web/error/ErrorCode.java` → `PostErrorCode.java` | web | переименовать, изменить | Р0-3, Р0-6 |
| 2.5 | `PostExceptionHandler.java`, `PostController.java`, `FeedController.java` | web | изменить | Р0-3, Р1-6, Р1-7 |
| 2.6 | `PostControllerTest.java` | test | изменить | Р1-1 |
| **Этап 3** | | | | |
| 3.1 | `docs/api/paths/followers-get.yaml`, `subscribes-get.yaml` | контракт | изменить | Р1-1, Р1-5, Р1-7, Р2-3 |
| 3.2 | `pom.xml` | генерация | изменить | Р0-5 |
| 3.3 | `subscriptions/.../web/error/ErrorCode.java` | web | удалить | Р0-6, Р1-7 |
| 3.4 | `SubscriptionExceptionHandler.java`, `SubscriptionController.java` | web | изменить | Р1-6, Р1-7 |
| 3.5 | `docs/api/schemas/error.yaml`, `shared/web/ErrorResponse.java` | контракт / shared | удалить | Р0-4, Р0-5 |

---

## Этап 1. Общая часть + friendships

### Контракт и генерация (п.1–3 задания)

**1.1.** Снят: общего перечня кодов в контракте нет, коды — в Java-enum'ах модулей (1.8, 1.14). → Р0-3, Р0-4

**1.2. `docs/api/schemas/api-error-response.yaml`** — контракт. Зачем: одна схема тела ошибки вместо копий в каждом ответе.
`required: [status, detail, code]`; `status` — `integer/int32`, `detail` — `string`, `code` — `string` с `description` «машинный код ошибки; возможные значения — в примерах ответов операции» и `example: VALIDATION_ERROR`. Без `enum`, без `title` (урок). → Р1-1, Р0-4

**1.3. `docs/api/responses/internal-error.yaml`** — контракт. Зачем: исход 500 есть у каждой операции, описать его один раз.
Объект Response: `description`, `content: application/problem+json`, `schema: $ref '../schemas/api-error-response.yaml'`, `example` `{status: 500, detail: ..., code: INTERNAL_ERROR}`. → Р1-10

**1.4. `docs/api/modules/shared.yaml`** — генерация. Зачем: общие классы генерируются один раз, а не в каждом модуле.
Минимальная спецификация: `openapi: 3.0.3`, `info`, `paths: {}`, `components.schemas` со ссылкой на файл 1.2. → Р0-5

**1.5. `pom.xml`** — генерация.
- в общих `configOptions` — `<generateGenericResponseEntity>true</generateGenericResponseEntity>` (Р1-6).
  Флаг общий, но posts и subscriptions от этого не сломаются: `ResponseEntity<PostWebResponse>` в контроллере — допустимый ковариантный тип для `ResponseEntity<?>` интерфейса;
- новый execution `shared-api-errors`: `inputSpec` — 1.4, `modelPackage` — `ru.job4j.socialmediaapi.shared.web`, `generateApis=false`, `modelsToGenerate` и `modelNameMappings` (`api-error-response=ApiErrorResponse`) — по образцу `friendships-responses` (Р0-5);
- только friendships: в `openapi.friendships.schemas` `error=ErrorResponse` → `api-error-response=ApiErrorResponse`; в `openapi.friendships.imports` — `ApiErrorResponse=…shared.web.ApiErrorResponse`, `ErrorResponse=…` убрать;
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
- в `target/generated-sources/openapi/ru/job4j/socialmediaapi/shared/web/` есть `ApiErrorResponse`, поле `code` — `String`;
- в пакетах friendships **нет** своей копии этого класса (если есть — не хватает `schemaMappings`/`importMappings`);
- методы `FriendshipOfferApi` и других `*Api` возвращают `ResponseEntity<?>`.

### Java: общая часть (п.6 задания)

**1.8. `shared/web/ErrorCode.java` и `shared/web/CommonErrorCode.java`** — shared. Зачем: коды живут в своих модулях, а тело и `Content-Type` собираются в одном месте; обработчики остаются однострочными (`XxxErrorCode.Y.response()`, как сейчас).
- `ErrorCode` — интерфейс: `String name()` (реализует сам enum), `HttpStatus status()`, `String detail()` и
  `default ResponseEntity<ApiErrorResponse> response()` — `ResponseEntity.status(status()).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(new ApiErrorResponse().status(status().value()).detail(detail()).code(name()))`;
- `CommonErrorCode implements ErrorCode` — `VALIDATION_ERROR(BAD_REQUEST, «Некорректное тело или параметры запроса»)` (один текст для всех модулей, Р1-7) и `INTERNAL_ERROR(INTERNAL_SERVER_ERROR, «Внутренняя ошибка сервера»)`.
Поля `status`/`detail` и конструктор — как в нынешних модульных `ErrorCode`. → Р0-3, Р1-5, Р1-7

**1.9. `shared/exception/GlobalExceptionHandler.java`** — shared.
- `extends ResponseEntityExceptionHandler` — ошибки Spring MVC (неизвестный URL, неверный метод, `Content-Type`/`Accept`) получают свой статус, а не 500 (Р3-1);
- `handleUnexpected(Exception)` → `LOG.error(...)` + `CommonErrorCode.INTERNAL_ERROR.response()`;
- `handleDataIntegrityViolation` оставить, перевести на `CommonErrorCode.INTERNAL_ERROR.response()`;
- строковую константу `INTERNAL_ERROR` удалить: код теперь `CommonErrorCode.INTERNAL_ERROR`.
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

**1.14. `friendships/.../web/error/ErrorCode.java` → `FriendshipOfferErrorCode.java`** — web. Переименовать (IDEA → Refactor → Rename), затем:
- `implements ErrorCode` (`shared.web`); `message` → `detail`; метод `response()` удалить — он теперь default в интерфейсе;
- удалить `INVALID_FRIENDSHIP_OFFER_REQUEST` (→ `CommonErrorCode.VALIDATION_ERROR`) и `FRIENDSHIP_OFFER_ALREADY_PENDING` (Р2-1);
- добавить `FRIENDSHIP_OFFER_ALREADY_EXISTS(CONFLICT, «Предложение дружбы для этой пары уже существует»)` (Р2-2);
- Javadoc класса: `INTERNAL_ERROR` и `VALIDATION_ERROR` — в `CommonErrorCode`.
→ Р0-3, Р0-6

**1.15. `FriendshipOfferExceptionHandler.java`** — web.
- возвращаемый тип — `ResponseEntity<ApiErrorResponse>`;
- `handleInvalidRequest` → `CommonErrorCode.VALIDATION_ERROR.response()`;
- `handleAlreadyPending` — удалить;
- `handleNotFound` / `handleAlreadyAccepted` / `handleAlreadyRejected` → `FriendshipOfferErrorCode.X.response()`;
- `handleAlreadyExists` → `FriendshipOfferErrorCode.FRIENDSHIP_OFFER_ALREADY_EXISTS.response()` + `LOG.warn` (Р2-2).
→ Р0-3, Р1-6, Р1-7, Р2-2

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
| `FriendshipOfferExceptionHandlerTest` | удалить кейс pending; `AlreadyExists` → 409 `FRIENDSHIP_OFFER_ALREADY_EXISTS`; `ErrorCode` → `FriendshipOfferErrorCode` / `CommonErrorCode` |
| `FriendshipOfferControllerTest` (MockMvc) | повтор при `PENDING` → 200 с телом предложения; `$.message` → `$.detail`, `+ $.status`; `Content-Type` problem+json |
| `FriendshipOfferE2ETest` | `ErrorResponse` → `ApiErrorResponse`; `INVALID_*` → `VALIDATION_ERROR`; pending → 200 с существующим предложением (у встречной — направление первой). 404 и 409 уже есть |
| `UnexpectedErrorE2ETest` (новый, `shared/exception`) | `@E2ETest` + `@MockitoBean GetFriendshipOffer`, который бросает `RuntimeException` → `GET /api/v1/friendship-offer/{id}` → 500 `INTERNAL_ERROR`, текста исключения в теле нет |

`@MockitoBean` создаёт отдельный Spring-контекст — поэтому 500 вынесен в свой класс.

**Конец этапа 1:** `./mvnw verify` зелёный (posts и subscriptions пока отвечают `{code, message}`).

---

## Этап 2. posts

**2.1.** Снят: в контракте перечня кодов нет, `POST_NOT_FOUND` остаётся в `PostErrorCode` (2.4). → Р0-4

**2.2. `docs/api/paths/post-get.yaml`, `feed-get.yaml`** — как в 1.6: `problem+json`, `$ref` на `api-error-response.yaml`, примеры `{status, detail, code}`, 400 → `VALIDATION_ERROR`, + `'500'`. → Р1-1, Р1-5, Р1-7

**2.3. `pom.xml`** — `openapi.posts.schemas` и `openapi.posts.imports` — как для friendships в 1.5. → Р0-5

**2.4. `posts/.../web/error/ErrorCode.java` → `PostErrorCode.java`** — как в 1.14: `implements ErrorCode`, `message` → `detail`, `response()` удалить, `INVALID_POST_REQUEST` удалить (→ `CommonErrorCode.VALIDATION_ERROR`), остаётся `POST_NOT_FOUND`. → Р0-3, Р0-6

**2.5. `PostExceptionHandler.java`** — тип `ResponseEntity<ApiErrorResponse>`; 400 → `CommonErrorCode.VALIDATION_ERROR.response()`, `PostNotFoundException` → `PostErrorCode.POST_NOT_FOUND.response()`.
**`PostController.java`, `FeedController.java`** — `ResponseEntity<?>`. → Р1-6, Р1-7

**2.6. `PostControllerTest.java`** — формат тела (`detail`, `status`), код 400 → `VALIDATION_ERROR`. → Р1-1

**Конец этапа 2:** `./mvnw verify` зелёный.

---

## Этап 3. subscriptions + очистка

**3.1. `docs/api/paths/followers-get.yaml`, `subscribes-get.yaml`** — как в 1.6; удалить `404 USER_NOT_FOUND` (Р2-3); 400 → `VALIDATION_ERROR`. → Р1-1, Р1-5, Р1-7, Р2-3

**3.2. `pom.xml`** — `openapi.subscriptions.schemas` и `openapi.subscriptions.imports` — как в 1.5. → Р0-5

**3.3. `subscriptions/.../web/error/ErrorCode.java`** — удалить: единственный код `INVALID_USER_ID` → `CommonErrorCode.VALIDATION_ERROR`; `SubscriptionErrorCode` не создаём, пока нет своих кодов. → Р0-6, Р1-7

**3.4. `SubscriptionExceptionHandler.java`** — тип `ResponseEntity<ApiErrorResponse>`; 400 → `CommonErrorCode.VALIDATION_ERROR.response()`.
**`SubscriptionController.java`** — `ResponseEntity<?>`. → Р1-6
Web-тестов у subscriptions нет, новые не пишем — 400 проверяется ручными тестами.

**3.5. Очистка:**
- `docs/api/schemas/error.yaml` — удалить, если `grep -r "schemas/error.yaml" docs/api` пуст (Р0-4);
- `shared/web/ErrorResponse.java` — удалить, если `grep -r "web.ErrorResponse" src` пуст (Р0-5).

**Конец этапа 3:** `./mvnw verify` зелёный.

---

## После этапов

- `ApplicationModulesTest` — по команде (затронуты `shared` и все модули).
- П.5 задания: `grep -rn "DataAccessException\|DataIntegrityViolation" src/main/java` — вне `persistence/adapter` и `GlobalExceptionHandler` их быть не должно.
- Ручные тесты — `manual-test-curl.md`, `manual-test-bruno.md` и коллекция `bruno/41224/`: 201/200 create, 404/409 accept, 400 `VALIDATION_ERROR` в каждом модуле, `Content-Type: application/problem+json`. Готовлю я по команде.
