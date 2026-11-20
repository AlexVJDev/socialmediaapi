# 40001 — журнал решений

| ID | Решение (1–2 строки) | Источник | Статус |
|---|---|---|---|
| Р0-1 | Действующий пользователь create/accept/reject — из `CurrentUserProvider` (`X-User-Id`); выделено из 40000 Р1-3. | descr.md, 2026-11-19 | действует |
| Р1-1 | `fromUserId` (create) и `actorId` (accept/reject) убираются из тела сразу; в yaml — `security: - userIdHeader: []` и `'401': $ref: '../responses/unauthorized.yaml'`. | fix.md в.1, 2026-11-20 | действует |
| Р1-2 | Старое поле в теле игнорируется (Jackson по умолчанию); источник — только `X-User-Id`. | fix.md в.2, 2026-11-20 | действует |
| Р1-3 | Ответы create/accept/reject не меняются. | fix.md в.3, 2026-11-20 | действует |
| Р1-4 | Команда собирается MapStruct из двух источников (`toCommand(UUID fromUserId, request)`, `toAcceptCommand/toRejectCommand(UUID actorId, request)`); команды не меняются. | fix.md в.4, 2026-11-20 | действует |
| Р1-5 | Невалидное тело и нет `X-User-Id` → `400` (не `401`) — оставить до Keycloak, описать в таблице ошибок `friendship-offer-{create,accept,reject}.md`. | fix.md в.5, 2026-11-20 | действует |
| Р1-6 | `X-User-Id` = `toUserId` → `400 VALIDATION_ERROR`, как сейчас; переформулировать описание `400`. | fix.md в.6, 2026-11-20 | действует |
| Р1-7 | Тесты под заголовок: `FriendshipOfferControllerTest`, `FriendshipOfferE2ETest`, `FriendshipOfferWebMapperTest`, `FriendshipFeedTest`, `FriendshipE2ETest`, `FriendshipOfferGetControllerTest`; новые кейсы в `FriendshipOfferE2ETest`: create без `X-User-Id` → `401`, accept с чужим `X-User-Id` → `404`. | fix.md в.7, 2026-11-20 | действует |
| Р1-8 | Обновить `docs/md/api-manual-test/{friendship-offer,feed,subscription}.md` и `bruno/40000/01–04`; новая коллекция — `bruno/40001/`. | fix.md в.8, 2026-11-20 | действует |
| Р1-9 | 40001 начинается сейчас, отдельно от правок по code-review 40000. | fix.md в.9, 2026-11-20 | действует |
| Р1-10 | После проверки плана Claude выполняет все пункты подряд, вопросы — в конце. | fix.md в.10, 2026-11-20 | действует |
| Р2-1 | `docs/md/task/40000/manual-test-curl.md` и `manual-test-bruno.md` переводятся на `X-User-Id` в create/accept (как Р1-8). | чат 2026-11-20 | действует |
| Р2-2 | В `docs/md/api-manual-test/friendship-offer.md` код `INVALID_FRIENDSHIP_OFFER_REQUEST` → `VALIDATION_ERROR`, формат тела ошибки → `{status, detail, code}`. | чат 2026-11-20 | действует |
| Р3-1 | `docs/process/FriendshipRequest.md`: действующий пользователь — `CurrentUserProvider` (`X-User-Id`), не тело; `actorId` — поле команды. | code-review.md п.1, 2026-11-20 | действует |
| Р3-2 | `db/dev/swagger-examples.sql`: комментарий `actorId = B` → `X-User-Id = B`. | code-review.md п.2, 2026-11-20 | действует |
| Р3-3 | В yaml accept/reject ответ `'401'` — сразу после `'400'`. | code-review.md п.3, 2026-11-20 | действует |
| Р3-4 | Автотесты `401` для accept/reject не добавляются (Р1-7). | code-review.md п.4, 2026-11-20 | действует |
| Р3-5 | `friendship_transitions.puml` не меняется: `actorId` — поле команды. | code-review.md п.5, 2026-11-20 | действует |
| Р4-1 | `db/dev/swagger-examples.sql`: часть 1 удаляет данные пользователей-примеров (A, B, C, D, E, P, G(1..100)), часть 2 вставляет; повторный прогон даёт тот же результат. | чат 2026-11-20 | действует |
| Р4-2 | Основной пользователь Swagger — Алиса `0b6f…4a01`: от её имени работают все запросы с примерами; второй — Борис `5e2a…1e22`. Описано в README («Users for Swagger UI»). | чат 2026-11-20 | действует |
| Р4-3 | Все GET с примерами возвращают ≥10 элементов; примеры контракта под данные: `direction` = `INCOMING`, create `toUserId` = E, accept/reject — входящие заявки Алисы `0f…01`/`0f…02`, `users/{userId}/posts` и `post.authorId` — Алиса. | чат 2026-11-20 | действует |
| Р4-4 | В `docs/api/friendship-offer-{create,accept,reject}.md` примеры id и времени — как в yaml (Р4-3): create `toUserId` = E, accept/reject — `X-User-Id` Алиса, заявки `0f…01`/`0f…02`. | чат 2026-11-20 | действует |
| Р5-1 | Статика `/openapi/**` отдаётся с `Cache-Control: no-cache` (`spring.web.resources.cache.cachecontrol.no-cache`), тест в `OpenApiE2ETest`; в README — `Ctrl+F5` для старой копии. | review_2.md, 2026-11-20 | действует |
