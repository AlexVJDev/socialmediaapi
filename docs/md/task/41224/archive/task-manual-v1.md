# 41224 — HTTP-статусы процесса. План для ручной реализации

Режим Ментора: ДА. Эталон — `decisions.md`; при расхождении плана с решением прав `decisions.md`.

Итого: **6 новых**, **6 удаляемых**, **~35 изменяемых** файлов (12 path-файлов контракта, 3 md-описания, pom, 11 Java, 8 тестов).

## Где будут файлы

```
docs/api/
├── modules/shared.yaml                      (новый) спецификация только для генерации общих моделей
├── responses/internal-error.yaml            (новый) общий ответ 500
├── schemas/api-error-code.yaml              (новый) enum кодов
├── schemas/api-error-response.yaml          (новый) тело ошибки
├── schemas/error.yaml                       (удалить)
└── paths/*.yaml                             (12 файлов — $ref, problem+json, примеры)

src/main/java/ru/job4j/socialmediaapi/
├── shared/web/ApiErrors.java                (новый) фабрика ResponseEntity<ApiErrorResponse>
├── shared/web/ErrorResponse.java            (удалить)
├── shared/exception/GlobalExceptionHandler.java
├── friendships/internal/...                 create-сценарий, advice, контроллер, ErrorCode (удалить)
├── posts/internal/infrastructure/web/...    advice, контроллеры, ErrorCode (удалить)
└── subscriptions/internal/infrastructure/web/...  advice, контроллер, ErrorCode (удалить)

target/generated-sources/openapi/ru/job4j/socialmediaapi/shared/web/
└── ApiErrorResponse.java, ApiErrorCode.java (генерируются)

src/test/java/ru/job4j/socialmediaapi/
└── shared/exception/UnexpectedErrorE2ETest.java  (новый) E2E на 500
```

## Файлы

| № | Файл | Слой | Действие | Решение |
|---|---|---|---|---|
| 1 | `docs/api/schemas/api-error-code.yaml` | контракт | новый | Р1-2, Р1-3, Р2-4 |
| 2 | `docs/api/schemas/api-error-response.yaml` | контракт | новый | Р1-1, Р1-3 |
| 3 | `docs/api/responses/internal-error.yaml` | контракт | новый | Р1-10 |
| 4 | `docs/api/paths/*.yaml` (12) | контракт | изменить | Р1-1, Р1-5, Р1-7, Р1-8, Р2-2, Р2-3 |
| 5 | `docs/api/schemas/error.yaml` | контракт | удалить | Р1-3 |
| 6 | `docs/api/friendship-offer-{create,accept,reject}.md` | документация | изменить | Р1-1, Р1-7, Р2-3 |
| 7 | `docs/api/modules/shared.yaml` | генерация | новый | Р1-4 |
| 8 | `pom.xml` | генерация | изменить | Р1-4, Р1-6, Р2-5 |
| 9 | `shared/web/ApiErrors.java` | shared / web | новый | Р1-4, Р1-5, Р1-7 |
| 10 | `shared/exception/GlobalExceptionHandler.java` | shared / web | изменить | Р1-10, Р3-1 |
| 11 | `friendships/.../model/CreateFriendshipOfferResult.java` | application | изменить | Р2-1 |
| 12 | `friendships/.../mapper/FriendshipOfferApplicationMapper.java` | application | изменить | Р2-1 |
| 13 | `friendships/.../usecase/CreateFriendshipOffer.java` | application | изменить | Р2-1 |
| 14 | `friendships/.../domain/exception/FriendshipOfferAlreadyPendingException.java` | domain | удалить | Р2-1 |
| 15 | `friendships/.../web/error/ErrorCode.java` | infrastructure / web | удалить | Р1-2 |
| 16 | `friendships/.../web/FriendshipOfferExceptionHandler.java` | infrastructure / web | изменить | Р1-6, Р1-7, Р2-2 |
| 17 | `friendships/.../web/FriendshipOfferController.java` | infrastructure / web | изменить | Р1-6, Р2-1 |
| 18 | `posts/.../web/error/ErrorCode.java` | infrastructure / web | удалить | Р1-2 |
| 19 | `posts/.../web/PostExceptionHandler.java` | infrastructure / web | изменить | Р1-6, Р1-7 |
| 20 | `posts/.../web/PostController.java`, `FeedController.java` | infrastructure / web | изменить | Р1-6 |
| 21 | `subscriptions/.../web/error/ErrorCode.java` | infrastructure / web | удалить | Р1-2 |
| 22 | `subscriptions/.../web/SubscriptionExceptionHandler.java` | infrastructure / web | изменить | Р1-6, Р1-7 |
| 23 | `subscriptions/.../web/SubscriptionController.java` | infrastructure / web | изменить | Р1-6 |
| 24 | `shared/web/ErrorResponse.java` | shared / web | удалить | Р1-4 |
| 25 | тесты (8) + `UnexpectedErrorE2ETest` | test | изменить / новый | Р1-1, Р1-10, Р2-1 |

---

## Подзадача 1. Контракт (п.1–2 задания)

**1. `docs/api/schemas/api-error-code.yaml`** — контракт. Зачем: закрытый перечень машинных кодов вместо произвольной строки.
`type: string`, `enum` — ровно 7 кодов из Р2-4. Новый код потом добавляется сначала сюда. → Р1-2, Р2-4

**2. `docs/api/schemas/api-error-response.yaml`** — контракт. Зачем: одна схема тела ошибки вместо копий в каждом ответе.
`required: [status, detail, code]`; `status` — `integer/int32`, `detail` — `string`, `code` — `$ref: './api-error-code.yaml'`. Без `title` (урок). → Р1-1

**3. `docs/api/responses/internal-error.yaml`** — контракт. Зачем: исход 500 есть у каждой операции, описать его один раз.
Объект Response: `description`, `content: application/problem+json` со `schema: $ref '../schemas/api-error-response.yaml'` и `example` `{status: 500, detail: ..., code: INTERNAL_ERROR}`. В path-файлах — `'500': $ref: '../responses/internal-error.yaml'`. → Р1-10

**4. `docs/api/paths/*.yaml` (12 файлов)** — контракт. Зачем: все исходы операции видны клиенту в одном формате.
В каждом ответе 4xx:
- `application/json` → `application/problem+json` (Р1-5);
- `$ref: '../schemas/error.yaml'` → `'../schemas/api-error-response.yaml'`;
- пример → `{status, detail, code}`; все 400 — `VALIDATION_ERROR` (Р1-7).

Отдельно:
- 6 файлов с `404 USER_NOT_FOUND` — удалить этот ответ (Р2-3): `friendship-offer-create`, `friends-get`, `followers-get`, `subscribes-get`, `friendships-incoming-get`, `friendships-outgoing-get`;
- `friendship-offer-create.yaml`: `'200'` — та же схема, что у `'201'` (вынести схему в `schemas/` или сослаться на неё), описание — «предложение пары уже ждёт решения, возвращается существующее» (Р1-8, Р2-5); в `'409'` добавить пример `FRIENDSHIP_OFFER_ALREADY_EXISTS` (Р2-2);
- `accept`/`reject`: `409` уже есть — только `$ref` и формат (п.1 задания: повтор — это `409`, а не `204`);
- `'500'` — во все операции, через п.3.

Подсказка: `examples` с несколькими значениями (как сейчас в `409` accept) сохраняются — меняется только форма значения.

**5. `docs/api/schemas/error.yaml`** — удалить, когда в п.4 не останется ссылок (`grep -r error.yaml docs/api`). → Р1-3

**6. `docs/api/friendship-offer-{create,accept,reject}.md`** — документация. Таблицы кодов: `message` → `detail`, `INVALID_*` → `VALIDATION_ERROR`, убрать `USER_NOT_FOUND` и `FRIENDSHIP_OFFER_ALREADY_PENDING`, у create — `200` с телом предложения. → Р1-1, Р1-7, Р2-1, Р2-3

## Подзадача 2. Генерация (п.3 задания)

**7. `docs/api/modules/shared.yaml`** — генерация. Зачем: из общих схем генерируются классы один раз, не в каждом модуле.
Минимальная спецификация: `openapi: 3.0.3`, `info`, `paths: {}`, `components.schemas` со ссылками на оба файла из п.1–2. → Р1-4

**8. `pom.xml`** — генерация.
- в общих `configOptions` — `<generateGenericResponseEntity>true</generateGenericResponseEntity>` (Р1-6);
- новый execution `shared-api-errors`: `inputSpec` — п.7, `modelPackage` — `ru.job4j.socialmediaapi.shared.web`, `generateApis=false`, `modelsToGenerate` и `modelNameMappings` (`api-error-response=ApiErrorResponse,api-error-code=ApiErrorCode`) — по образцу `friendships-responses` (Р1-4);
- в `openapi.{friendships,subscriptions,posts}.schemas`: `error=ErrorResponse` → `api-error-response=ApiErrorResponse,api-error-code=ApiErrorCode`; в `*.imports` — `ApiErrorResponse=…shared.web.ApiErrorResponse`, `ApiErrorCode=…shared.web.ApiErrorCode`, строку `ErrorResponse=…` убрать;
- убрать `createFriendshipOffer_200_response` из `openapi.friendships.schemas` и комментарий-обоснование (Р2-5).

Проверка: `./mvnw clean generate-sources` (Р1-12), затем:
- в `target/generated-sources/openapi/ru/job4j/socialmediaapi/shared/web/` есть `ApiErrorResponse` и `ApiErrorCode`;
- в пакетах модулей **нет** своих копий этих классов (если есть — не хватает `schemaMappings`/`importMappings`);
- методы `*Api` возвращают `ResponseEntity<?>`.

После этого проект не компилируется — это ожидаемо, чинится подзадачами 3–6.

## Подзадача 3. Общие обработчики (п.6 задания)

**9. `shared/web/ApiErrors.java`** — shared. Зачем: собрать тело и `Content-Type` в одном месте, чтобы advice модулей были однострочными.
`final`-класс со статическими методами:
- `response(HttpStatus status, ApiErrorCode code, String detail)` → `ResponseEntity<ApiErrorResponse>` с `contentType(MediaType.APPLICATION_PROBLEM_JSON)` и телом `new ApiErrorResponse().status(status.value()).detail(detail).code(code)`;
- `invalidRequest()` → `400 VALIDATION_ERROR` с общим текстом «Некорректное тело или параметры запроса» (один текст на три модуля, Р1-7).
Это решение плана (вместо модульных `ErrorCode`), можно сделать иначе — тогда запишем в `decisions.md`. → Р1-4, Р1-5, Р1-7

**10. `shared/exception/GlobalExceptionHandler.java`** — shared.
- `extends ResponseEntityExceptionHandler` (Р3-1 — **ждёт ответа в fix.md**);
- `handleUnexpected(Exception)` → `LOG.error(...)` + `ApiErrors.response(INTERNAL_SERVER_ERROR, INTERNAL_ERROR, «Внутренняя ошибка сервера»)`;
- `handleDataIntegrityViolation` оставить, перевести на `ApiErrors`;
- константы `INTERNAL_ERROR`/`INTERNAL_ERROR_MESSAGE`: код теперь `ApiErrorCode.INTERNAL_ERROR`, строковую константу кода удалить.
Не возвращать в `detail` `exception.getMessage()` — там могут быть SQL и имена классов. → Р1-10

## Подзадача 4. friendships (п.4–5 задания)

**11. `CreateFriendshipOfferResult.java`** — application. Добавить `boolean created` (`true` — создано, `false` — возвращено существующее `PENDING`). → Р2-1

**12. `FriendshipOfferApplicationMapper.java`** — application.
- `toResult(CreateFriendshipOfferResponse)` → `created = true` (`@Mapping(target = "created", constant = "true")`);
- новый метод из `FindFriendshipOfferResponse` в `CreateFriendshipOfferResult` с `created = false`.
Имя второго метода не должно совпадать по сигнатуре с существующим `toResult(FindFriendshipOfferResponse)` → например, `toExistingResult`. → Р2-1

**13. `CreateFriendshipOffer.java`** — application.
При `FriendshipOfferAlreadyExistsException` читается пара (`findByPair`):
- `PENDING` → **вернуть** `toExistingResult(existing)`, подписку **не** создавать (ничего не изменилось);
- `ACCEPTED` / `REJECTED` → бросить, как сейчас;
- не прочиталось → бросить исходное `FriendshipOfferAlreadyExistsException`.
`alreadyExistsError` перестаёт подходить по смыслу (теперь он и возвращает результат) — переделать структуру метода. Обновить Javadoc (`@throws` без `AlreadyPending`, описание `created`).
Подсказка: insert откатывается до savepoint, поэтому нормальный возврат из `@Transactional` ничего лишнего не закоммитит — проверьте это тестом «данные не меняются». → Р2-1

**14. `FriendshipOfferAlreadyPendingException.java`** — удалить. → Р2-1

**15. `friendships/.../web/error/ErrorCode.java`** — удалить, вместо него `ApiErrorCode` + `ApiErrors`. → Р1-2

**16. `FriendshipOfferExceptionHandler.java`** — web.
- возвращаемый тип — `ResponseEntity<ApiErrorResponse>`;
- `handleInvalidRequest` → `ApiErrors.invalidRequest()`;
- `handleAlreadyPending` — удалить;
- `handleNotFound` / `handleAlreadyAccepted` / `handleAlreadyRejected` → `ApiErrors.response(...)` с прежними статусами и текстами;
- `handleAlreadyExists` → `409 FRIENDSHIP_OFFER_ALREADY_EXISTS` + `LOG.warn` (Р2-2).
→ Р1-6, Р1-7, Р2-2

**17. `FriendshipOfferController.java`** — web.
- все методы — `ResponseEntity<?>`;
- create: `result.created() ? 201 : 200`, тело в обоих случаях — `mapper.toResponse(result)`;
- Javadoc: убрать упоминания `FRIENDSHIP_OFFER_ALREADY_PENDING`.
Если MapStruct предупредит о неиспользуемом `created` в `FriendshipOfferWebMapper` — добавить `@Mapping(target=..., ignore=...)`, по умолчанию лишние поля источника игнорируются. → Р1-6, Р2-1

## Подзадача 5. posts и subscriptions

**18. `posts/.../web/error/ErrorCode.java`** — удалить. → Р1-2
**19. `PostExceptionHandler.java`** — 400 → `ApiErrors.invalidRequest()`, `PostNotFoundException` → `404 POST_NOT_FOUND`. → Р1-6, Р1-7
**20. `PostController.java`, `FeedController.java`** — `ResponseEntity<?>`. → Р1-6
**21. `subscriptions/.../web/error/ErrorCode.java`** — удалить (`INVALID_USER_ID` → `VALIDATION_ERROR`). → Р1-2, Р1-7
**22. `SubscriptionExceptionHandler.java`** — 400 → `ApiErrors.invalidRequest()`. → Р1-6, Р1-7
**23. `SubscriptionController.java`** — `ResponseEntity<?>`. → Р1-6

**24. `shared/web/ErrorResponse.java`** — удалить последним, когда `grep -r "web.ErrorResponse" src` пуст. → Р1-4

## Подзадача 6. Тесты (п.7 задания)

**25.** Тесты — минимально, только то, что ломается или требует задание:

| Тест | Что поменять |
|---|---|
| `GlobalExceptionHandlerTest` | тело `ApiErrorResponse`; + кейс `handleUnexpected` → 500 `INTERNAL_ERROR` |
| `CreateFriendshipOfferTest` | повтор и встречное при `PENDING` → результат с `created=false`, без исключения, подписка не вызывается |
| `CreateFriendshipOfferConcurrencyTest` | ожидание `AlreadyPending` → один `created=true`, остальные `created=false` |
| `FriendshipOfferApplicationMapperTest` | `created` в обоих маппингах (если тест покрывает `toResult`) |
| `FriendshipOfferExceptionHandlerTest` | удалить кейс pending; `AlreadyExists` → 409 `FRIENDSHIP_OFFER_ALREADY_EXISTS` |
| `FriendshipOfferControllerTest` (MockMvc) | повтор при `PENDING` → 200 с телом предложения; `$.message` → `$.detail`, `+ $.status`; `Content-Type` problem+json |
| `FriendshipOfferE2ETest` | `ErrorResponse` → `ApiErrorResponse`; `INVALID_*` → `VALIDATION_ERROR`; pending → 200 с существующим предложением (у встречной — направление первой). 404 и 409 уже есть |
| `PostControllerTest` | формат тела и код 400 |
| `UnexpectedErrorE2ETest` (новый, `shared/exception`) | `@E2ETest` + `@MockitoBean GetPost`, который бросает `RuntimeException` → `GET /api/v1/posts/{id}` → 500 `INTERNAL_ERROR`, в теле нет текста исключения |

Подсказка: `@MockitoBean` создаёт отдельный Spring-контекст — поэтому 500 вынесен в свой класс, а не в `FriendshipOfferE2ETest`.

## Подзадача 7. Проверка и ручные тесты

**26.** `./mvnw verify` (тесты + checkstyle). Затем — `ApplicationModulesTest` (по команде; затронуты `shared` и все модули).
**27.** Проверка п.5 задания: `grep -rn "DataAccessException\|DataIntegrityViolation" src/main/java` — вне `persistence/adapter` и `GlobalExceptionHandler` их быть не должно.
**28.** Ручные тесты — `manual-test-curl.md` и `manual-test-bruno.md` + коллекция `bruno/41224/`: 201/200 create, 404/409 accept, 400 `VALIDATION_ERROR`, проверка `Content-Type: application/problem+json`. Готовлю я, по команде.
