# 41224 — журнал решений

| ID | Решение (1–2 строки) | Источник | Статус |
|---|---|---|---|
| Р0-1 | Режим ментора: план — один `task-manual.md` для пользователя, код пишет пользователь | чат, 13.11.2026 | действует |
| Р0-2 | Изменения делаются в 3 этапа, по одному модулю за этап (порядок предложен в плане: friendships → posts → subscriptions); после каждого этапа сборка и тесты зелёные; общая часть — в первом этапе, удаление старого — в последнем | чат, 13.11.2026 | действует |
| Р1-1 | Тело ошибки — `{status, detail, code}` как в уроке; `{code, message}` убрать, тесты поправить | fix.md Раунд 1, в.1 | действует |
| Р1-2 | Один общий `ApiErrorCode` (enum в контракте) с кодами проекта под нашими именами + `VALIDATION_ERROR`, `INTERNAL_ERROR`; неиспользуемые коды (`ACCESS_DENIED`) не добавлять | fix.md Раунд 1, в.2 | заменено Р0-3 |
| Р1-3 | Схемы — `docs/api/schemas/api-error-code.yaml`, `api-error-response.yaml` (имя класса через `model-names`); `error.yaml` удалить | fix.md Раунд 1, в.3 | заменено Р0-4 |
| Р1-4 | `ApiErrorResponse`/`ApiErrorCode` генерируются один раз отдельным execution в пакет `ru.job4j.socialmediaapi.shared.web`; модули подключают через `importMappings`; рукописный `ErrorResponse` удалить | fix.md Раунд 1, в.4 | заменено Р0-5 |
| Р1-5 | Content-Type ошибок — `application/problem+json` (в контракте и в ответах) | fix.md Раунд 1, в.5 | действует |
| Р1-6 | Ожидаемые ошибки — через модульные `@RestControllerAdvice` (указание ментора), `try/catch` в endpoint не вводить; исключения — `RuntimeException`; `generateGenericResponseEntity` включить | fix.md Раунд 1, в.6 | действует |
| Р1-7 | Все ошибки 400 — один код `VALIDATION_ERROR`; обработчики остаются в модульных advice | fix.md Раунд 1, в.7 | действует |
| Р1-8 | Повторное/встречное предложение при `PENDING` — `200` с телом существующего предложения (идемпотентно), вместо `200` + `FRIENDSHIP_OFFER_ALREADY_PENDING` | fix.md Раунд 1, в.8 | заменено Р4-1 |
| Р1-9 | Объём — все контроллеры: friendships, posts, subscriptions (по подзадаче на модуль); `501`-заглушки не трогать | fix.md Раунд 1, в.9 | действует |
| Р1-10 | 500 — расширить `shared.exception.GlobalExceptionHandler` (`Exception.class` → `INTERNAL_ERROR` + лог, `DataIntegrityViolation` оставить); E2E на 500 — отдельный класс с `@MockitoBean` на use case | fix.md Раунд 1, в.10 | действует |
| Р1-11 | Пути и имена path-файлов проекта (`paths/friendship-offer-accept.yaml` …) не менять под урок | fix.md Раунд 1, к сведению | действует |
| Р1-12 | Генератор: `useSpringBoot4` не менять; сборка — `./mvnw generate-sources` | fix.md Раунд 1, к сведению | действует |
| Р1-13 | Чужая заявка — `404` (существование не раскрывается), `403` не вводить | fix.md Раунд 1, к сведению | действует |
| Р2-1 | Create при `PENDING` пары возвращает существующее предложение с `created=false` в `CreateFriendshipOfferResult`; контроллер выбирает `201`/`200`; подписку не создавать; `FriendshipOfferAlreadyPendingException` удалить | fix.md Раунд 2, в.1 | заменено Р4-1 |
| Р2-2 | `FriendshipOfferAlreadyExistsException` (дубликат не прочитан) → `409` с новым кодом `FRIENDSHIP_OFFER_ALREADY_EXISTS` + warn в лог | fix.md Раунд 2, в.2 | действует |
| Р2-3 | `404 USER_NOT_FOUND` убрать из контракта (6 path-файлов), в `ApiErrorCode` не добавлять | fix.md Раунд 2, в.3 | действует |
| Р2-4 | Состав `ApiErrorCode`: `VALIDATION_ERROR`, `FRIENDSHIP_OFFER_NOT_FOUND`, `FRIENDSHIP_OFFER_ALREADY_ACCEPTED`, `FRIENDSHIP_OFFER_ALREADY_REJECTED`, `FRIENDSHIP_OFFER_ALREADY_EXISTS`, `POST_NOT_FOUND`, `INTERNAL_ERROR` | fix.md Раунд 2, к сведению | заменено Р0-6 |
| Р2-5 | Обходной маппинг `createFriendshipOffer_200_response` в pom убрать: 200 и 201 create ссылаются на одну схему | fix.md Раунд 2, к сведению | заменено Р4-3 |
| Р0-3 | Коды ошибок — по модулям: интерфейс `shared.web.ErrorCode` (`name()`, `status()`, `detail()`, default `response()`); `CommonErrorCode` (`VALIDATION_ERROR`, `INTERNAL_ERROR`) в `shared.web`; модульные `FriendshipOfferErrorCode`, `PostErrorCode` в `<module>/internal/infrastructure/web/error` (имя `<Module>ErrorCode`, единственное число) | чат, 15.11.2026 | действует |
| Р0-4 | Контракт: одна схема `docs/api/schemas/api-error-response.yaml`, `code` — `string` без enum (коды операции — в примерах её path-файла); `api-error-code.yaml` не создаётся; `error.yaml` удалить | чат, 15.11.2026 | действует |
| Р0-5 | Генерируется только `ApiErrorResponse` (отдельный execution, пакет `shared.web`); модули подключают через `importMappings`; рукописный `ErrorResponse` удалить | чат, 15.11.2026 | действует |
| Р0-6 | Состав кодов: `FriendshipOfferErrorCode` — `FRIENDSHIP_OFFER_NOT_FOUND`, `_ALREADY_ACCEPTED`, `_ALREADY_REJECTED`, `_ALREADY_EXISTS`; `PostErrorCode` — `POST_NOT_FOUND`; у subscriptions своих кодов нет — `SubscriptionErrorCode` не создаётся до первого кода | чат, 15.11.2026 | заменено Р4-2 |
| Р3-1 | `GlobalExceptionHandler extends ResponseEntityExceptionHandler`: стандартные ошибки Spring MVC (404/405/406/415 и др.) отдаются со своим статусом и телом `ProblemDetail` Spring, а не `500 INTERNAL_ERROR`; модульные advice срабатывают первыми | fix.md Раунд 3, в.1 | действует |
| Р4-1 | Повтор/встречное предложение при `PENDING` — как было: `200` с телом ошибки `{status: 200, detail, code: FRIENDSHIP_OFFER_ALREADY_PENDING}` (`application/problem+json`); `FriendshipOfferAlreadyPendingException` остаётся, поле `created` не вводится, `CreateFriendshipOffer` не меняется | fix.md Раунд 4, в.1 (а) | действует |
| Р4-2 | Состав кодов: `FriendshipOfferErrorCode` — `FRIENDSHIP_OFFER_ALREADY_PENDING` (200), `_NOT_FOUND`, `_ALREADY_ACCEPTED`, `_ALREADY_REJECTED`, `_ALREADY_EXISTS`; `PostErrorCode` — `POST_NOT_FOUND`; `SubscriptionErrorCode` не создаётся до первого кода | следствие Р4-1, 15.11.2026 | действует |
| Р4-3 | Обходной маппинг `createFriendshipOffer_200_response` в pom убрать: `200` create ссылается на `api-error-response.yaml`, `201` — на схему предложения, тип метода — `ResponseEntity<?>` | следствие Р4-1, 15.11.2026 | действует |
| Р4-4 | План — один `task-manual.md`, 3 последовательных этапа по модулям; у каждого этапа свои «Где будут файлы», «Файлы» и итог, общих таблиц нет | fix.md Раунд 4, в.2 (б) | действует |
| Р4-5 | `200 FRIENDSHIP_OFFER_ALREADY_PENDING` отдаётся с `Content-Type: application/problem+json` через общий `ErrorCode.response()`, отдельного метода нет; Р2-2 (`409 FRIENDSHIP_OFFER_ALREADY_EXISTS`) подтверждено | чат, 15.11.2026 | действует |
| Р4-6 | План `task-manual.md` (3 этапа: friendships → posts → subscriptions) принят, реализацию ведёт пользователь | чат, 15.11.2026 | действует |
| Р4-7 | Пункты плана выполняет Claude — по одному, по команде пользователя; пользователь проверяет результат (уточняет Р0-1) | чат, 15.11.2026 | действует |
