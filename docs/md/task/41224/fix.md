# 41224 — HTTP-статусы процесса. Вопросы

Режим Ментора: ДА

## Раунд 4 — ответы перенесены в decisions.md Р4-1…Р4-4

<details><summary>Вопросы Раунда 4</summary>

## Раунд 4 — Требует ответа

**1. Повторное/встречное предложение дружбы при `PENDING`** (повтор вопроса 8 Раунда 1: там сейчас «Принимается (a)», а в `decisions.md` действуют Р1-8 и Р2-1 — вариант в).
Варианты: а) оставить как есть — `200` с телом ошибки `{status: 200, detail, code: FRIENDSHIP_OFFER_ALREADY_PENDING}`; б) `409 FRIENDSHIP_OFFER_ALREADY_PENDING`; в) `200` с телом существующего предложения (`created=false`), как в Р1-8/Р2-1.
**Рекомендация:** в) — успех не выглядит как ошибка; при а) Р1-8, Р2-1 заменяются, `FriendshipOfferAlreadyPendingException` и код `FRIENDSHIP_OFFER_ALREADY_PENDING` остаются.
Ответ: а)

**2. Разбивка плана на 3 последовательные реализации по модулям** (friendships → posts → subscriptions).
Варианты: а) 3 файла `task-manual-1-friendships.md`, `task-manual-2-posts.md`, `task-manual-3-subscriptions.md`, каждый самодостаточный (свои «Где будут файлы», «Файлы», проверка), `task-manual.md` — в `archive/`; б) один `task-manual.md`, этапы без общих таблиц.
**Рекомендация:** а) — каждый этап читается и сдаётся отдельно.
Ответ: б)

</details>

## Раунд 3 — ответы перенесены в decisions.md Р3-1

<details><summary>Вопросы Раунда 3</summary>

## Раунд 3 — Требует ответа

**1. Стандартные ошибки Spring MVC и обработчик `Exception.class` (Р1-10).** Advice с `Exception.class` перехватит и их: неизвестный URL (`NoResourceFoundException`), неверный метод, неподдерживаемые `Content-Type`/`Accept` — вместо 404/405/415/406 клиент получит `500 INTERNAL_ERROR`.
Варианты: а) `GlobalExceptionHandler extends ResponseEntityExceptionHandler` — у них свой статус, тело — `ProblemDetail` Spring; б) не учитывать, как в примере урока.
**Рекомендация:** а) — стандартный способ Spring, модульные advice (`HIGHEST_PRECEDENCE`) по-прежнему срабатывают первыми.
Ответ: Принимается

</details>

## Раунд 2 — ответы перенесены в decisions.md Р2-1…Р2-5

<details><summary>Вопросы Раунда 2</summary>

## Раунд 2 — Требует ответа

**1. Как сценарий create сообщает «предложение уже есть» (Р1-8).** Сейчас бросается `FriendshipOfferAlreadyPendingException`, advice отвечает 200.
Варианты: а) `CreateFriendshipOffer` при `PENDING` возвращает существующее предложение с флагом `created=false` в `CreateFriendshipOfferResult`, контроллер выбирает `201`/`200`; б) исключение несёт данные предложения, advice собирает успешное тело.
**Рекомендация:** а) — успех не идёт через исключение; подписку при этом не создавать (ничего не изменилось); `FriendshipOfferAlreadyPendingException` удалить.
Ответ: Принимается

**2. `FriendshipOfferAlreadyExistsException`** (дубликат есть, но прочитать его не удалось) — сейчас `200` + `FRIENDSHIP_OFFER_ALREADY_PENDING`; по Р1-8 тела предложения для ответа нет.
Варианты: а) `409` с новым кодом `FRIENDSHIP_OFFER_ALREADY_EXISTS` + warn в лог; б) не обрабатывать в модуле — уйдёт в `500 INTERNAL_ERROR`.
**Рекомендация:** а) — клиенту честно сообщается, что предложение для пары уже есть.
Ответ: Принимается

**3. `USER_NOT_FOUND`** указан в контракте как `404` в 6 path-файлах, но таблицы пользователей нет и ни один код его не бросает.
Варианты: а) убрать эти `404` из контракта, в `ApiErrorCode` не добавлять; б) оставить в контракте и добавить код в `ApiErrorCode` «на будущее».
**Рекомендация:** а) — по Р1-2 неиспользуемые коды не добавляем; контракт не должен обещать исход, которого нет.
Ответ: Принимается

## К сведению (Раунд 2)
- `ApiErrorCode` по итогам Р1-2/Р1-7 и вопросов выше: `VALIDATION_ERROR`, `FRIENDSHIP_OFFER_NOT_FOUND`, `FRIENDSHIP_OFFER_ALREADY_ACCEPTED`, `FRIENDSHIP_OFFER_ALREADY_REJECTED`, `FRIENDSHIP_OFFER_ALREADY_EXISTS`, `POST_NOT_FOUND`, `INTERNAL_ERROR`. Уходят: `INVALID_*_REQUEST`, `INVALID_USER_ID`, `FRIENDSHIP_OFFER_ALREADY_PENDING`.
- `generateGenericResponseEntity` в openapi-generator 7.27.0 есть (проверено по jar) — методы станут `ResponseEntity<?>`.
- `problem+json` (Р1-5): клиент, присылающий строго `Accept: application/json`, получит на ошибку `406` вместо тела. curl (`*/*`) и `TestRestTemplate` (`application/*+json`) не затронуты.
- Обходной маппинг `createFriendshipOffer_200_response` в pom станет не нужен: 200 и 201 в create будут ссылаться на одну схему.

</details>

## Раунд 1 — ответы перенесены в decisions.md Р1-1…Р1-13

<details><summary>Вопросы Раунда 1</summary>

## Раунд 1 — Требует ответа

**1. Формат тела ошибки.** Сейчас `{code, message}` (`shared.web.ErrorResponse`), в уроке — `{status, detail, code}`.
Варианты: а) как в уроке; б) оставить `{code, message}`, только типизировать `code`.
**Рекомендация:** а) — это суть урока, ревьюер будет сверять с ним; тесты с `ErrorResponse` поправить.
Ответ: Принимается (а)

**2. Перечень кодов.** В уроке один общий `ApiErrorCode` (enum в контракте), у нас — `ErrorCode` в каждом модуле.
Имена кодов из урока (`FRIEND_REQUEST_*`, `ACCESS_DENIED`) у нас не используются.
**Рекомендация:** один `ApiErrorCode` со всеми кодами проекта под нашими именами (`FRIENDSHIP_OFFER_NOT_FOUND` …) + `VALIDATION_ERROR`, `INTERNAL_ERROR`; неиспользуемые (`ACCESS_DENIED`) не добавлять.
Ответ: делаем <module_name>ErrorCode 

**3. Где лежат схемы.** Урок: `docs/api/components/schemas/ApiErrorCode.yaml`; у нас — `docs/api/schemas/*.yaml` в kebab-case (`error.yaml`).
**Рекомендация:** по-нашему: `docs/api/schemas/api-error-code.yaml`, `api-error-response.yaml` (имя Java-класса — через `model-names`), `error.yaml` удалить.
Ответ: Принимается

**4. Где генерировать `ApiErrorResponse`/`ApiErrorCode`.** Генерация идёт отдельным execution на модуль — в каждом модуле появится своя копия класса.
**Рекомендация:** отдельный execution `shared-api-errors` с пакетом `ru.job4j.socialmediaapi.shared.web`; модули подключают классы через `importMappings`. Рукописный `ErrorResponse` удалить.
Ответ: Принимается

**5. Content-Type ошибок.** Урок — `application/problem+json`, у нас — `application/json`.
**Рекомендация:** как в уроке, `problem+json` (в контракте и в ответах через `contentType(MediaType.APPLICATION_PROBLEM_JSON)`).
Ответ: Принимается

**6. Как преобразовывать бизнес-ошибки.** Урок (п.4) — `try/catch` в endpoint; ментор — «все исключения `RuntimeException`, обрабатывать через ControllerAdvice».
У нас уже так, как говорит ментор: все исключения наследуют `RuntimeException`, в каждом модуле есть `*ExceptionHandler`.
**Рекомендация:** как говорит ментор — `try/catch` не вводить, оставить модульные advice и перевести их на `ApiErrorResponse`/`ApiErrorCode`. `generateGenericResponseEntity` (п.3) всё равно включить.
Ответ: Принимается

**7. Код для ошибок 400** (невалидное тело и параметры, предложение самому себе, неполный курсор). Сейчас в каждом модуле свой `INVALID_*_REQUEST`.
Варианты: а) один `VALIDATION_ERROR`, как в уроке; б) оставить `INVALID_*_REQUEST` по модулям.
**Рекомендация:** а); обработчики остаются в модульных advice, меняется только код.
Ответ: Принимается

**8. Повторное предложение дружбы при `PENDING`** — сейчас `200` с телом ошибки `FRIENDSHIP_OFFER_ALREADY_PENDING`. Урок: повтор — либо `409`, либо идемпотентный успех.
Варианты: а) оставить как есть (ваше решение); б) `409`; в) `200` с телом существующего предложения (идемпотентно).
**Рекомендация:** в) — с `ResponseEntity<?>` это стало возможно, и успех больше не выглядит как ошибка. Если хотите оставить а) — тоже ок.
Ответ: Принимается (a)

**9. Объём.** Урок говорит «в каждом endpoint».
**Рекомендация:** все контроллеры: friendships, posts, subscriptions (по подзадаче на модуль); `501`-заглушки не трогать.
Ответ: Принимается

**10. Обработчик 500 и его E2E-тест.** Урок предлагает новый `UnexpectedApiExceptionHandler`; у нас уже есть `shared.exception.GlobalExceptionHandler`.
**Рекомендация:** расширить существующий (`Exception.class` → 500 `INTERNAL_ERROR` + лог; обработчик `DataIntegrityViolation` оставить); E2E-тест 500 — отдельный класс с `@MockitoBean` на use case, бросающий `RuntimeException`.
Ответ: Принимается

## К сведению
- Пути урока не совпадают с проектом: `paths/friend-request/accept.yaml` → у нас `paths/friendship-offer-accept.yaml`. — не менять.
- `mvnw.cmd generate-sources` — на macOS `./mvnw generate-sources`; в уроке `useSpringBoot3`, у нас `useSpringBoot4` — не менять.
- Повторное принятие/отклонение уже возвращает `409` (`FRIENDSHIP_OFFER_ALREADY_ACCEPTED/REJECTED`) — п.1 задания по сути выполнен, остаётся перевести на `$ref`.
- Чужая заявка у нас — `404` (не раскрываем существование), а не `403` из урока — оставляем.
- П.5 задания (технические исключения репозитория) уже выполнен: дубликаты переводятся в исключения приложения в persistence-адаптерах (`PostgresErrors`); при ревью проверю ещё раз.
- E2E на 404 и 409 уже есть в `FriendshipOfferE2ETest`, потребуется только поменять ожидаемое тело.

</details>
