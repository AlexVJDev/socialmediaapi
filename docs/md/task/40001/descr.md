# 40001 — Операции заявок в друзья: действующий пользователь из `X-User-Id`

Правило проекта (Javadoc `shared/security/CurrentUserProvider`): пользователя, который выполняет запрос, сценарии и контроллеры
берут только из `CurrentUserProvider`, а не из тела или параметров запроса. Сейчас реализация — `HeaderCurrentUserProvider`
(заголовок `X-User-Id`), после подключения Keycloak — `sub` из JWT (меняется только реализация).
Модули posts и subscriptions правилу следуют, операции заявок в друзья — нет: действующий пользователь приходит в теле.

Выделено из задачи 40000 (Р1-3, 19.11.2026), где по этому правилу сразу делаются новые `incoming`/`outgoing`.

## 1. Что меняется

| № | Операция | Сейчас | Должно быть |
|---|---|---|---|
| 1 | `POST /api/v1/friendship-offer/create` — `createFriendshipOffer` | `fromUserId` в теле | отправитель — `X-User-Id`; в теле только `toUserId` |
| 2 | `POST /api/v1/friendship-offer/accept` — `acceptFriendshipOffer` | `actorId` в теле | действующий — `X-User-Id`; в теле только `offerId` |
| 3 | `POST /api/v1/friendship-offer/reject` — `rejectFriendshipOffer` | `actorId` в теле | как п. 2 |

Не меняется:
- `GET /api/v1/friendship-offer/{offerId}`, `GET /api/v1/friendship-offer` — чтение без проверки участника до Keycloak;
- публичные списки с `userId` в query: `friends`, `followers`, `subscribes` (GET), `posts-by-user`.

В описании `actorId` в контракте уже сказано: «передаётся в теле до подключения аутентификации (Keycloak), затем будет браться из токена».

## 2. Что понадобится (оценка)

- Контракт: `docs/api/paths/friendship-offer-{create,accept,reject}.yaml` — `security: - userIdHeader: []`, убрать поле из тела,
  добавить ответ `401` (как в `feed-get.yaml`); `docs/api/friendship-offer-{create,accept,reject}.md`.
- Код: `FriendshipOfferController` берёт действующего пользователя из `CurrentUserProvider` и передаёт в команды
  (`CreateFriendshipOfferCommand`, `Accept/RejectFriendshipOfferCommand` — поля остаются), маппинг web → application
  (`FriendshipOfferWebMapper`). Application, domain, persistence не меняются.
- `401` при отсутствии / не-UUID заголовка — уже обрабатывает `GlobalExceptionHandler` (`UnauthenticatedException`).
- Тесты: `FriendshipOfferControllerTest`, `FriendshipOfferE2ETest`, `FriendshipFeedTest`, `FriendshipOfferWebMapperTest`.
- Сценарии ручных запросов (connekt / Bruno), если в них есть create/accept/reject.

Всего ~10–14 файлов, в основном контракт и тесты.

## 3. Вопросы (решить в fix.md)

1. **Ответ create/accept/reject.** В теле ответа остаются `fromUserId`/`toUserId` (это данные заявки, а не действующий) — предложение: не трогать.
2. **Совместимость.** Ломающее изменение тела запроса: сразу убрать поле или на переходный период принимать и игнорировать?
   Предложение: убрать сразу (внешних клиентов нет).
3. **Старое поле в теле** (клиент прислал и `X-User-Id`, и `fromUserId`/`actorId`). В проекте `FAIL_ON_UNKNOWN_PROPERTIES` не задан —
   по умолчанию Spring Boot лишнее поле игнорирует. Предложение: так и оставить (`X-User-Id` — единственный источник).
4. **Порядок с 40000.** Делать после 40000 (там вводится `X-User-Id` в модуль friendships) — предложение: да.

## 4. Проверка

- Swagger UI: Authorize → `X-User-Id`, Try it out для create/accept/reject без поля в теле.
- Без заголовка → `401 UNAUTHORIZED`; чужой `X-User-Id` в accept/reject → `404 FRIENDSHIP_OFFER_NOT_FOUND`, как сейчас с чужим `actorId`.
- Ручные тесты по процессу: `manual-test-curl.md` и коллекция Bruno.
- `ApplicationModulesTest` — по команде пользователя.
