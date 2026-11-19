# 40000 — журнал решений

| ID | Решение (1–2 строки) | Источник | Статус |
|---|---|---|---|
| Р1-1 | Все три списка — страница с курсором вместо массива: параметры `limit`/`afterCreatedAt`/`afterId`, ответ — обёртка с полем `next` (контракт меняется). Детали — раунд 2. | fix.md в.1, 2026-11-19 | действует |
| Р1-2 | `incoming`/`outgoing` — в контракт добавляется query-параметр `status`. Детали (по умолчанию, допустимые значения) — раунд 2. | fix.md в.2, 2026-11-19 | действует |
| Р1-3 | `incoming`/`outgoing` — текущий пользователь из `CurrentUserProvider` (`X-User-Id`), query `userId` убирается из контракта; `friends` — `userId` в query (публичный список). Перевод create/accept/reject на `X-User-Id` — отдельная задача 40001. | чат 2026-11-19 | действует |
| Р1-4 | `404 «Пользователь не найден»` убирается из `friends-list.md`, `friendships-incoming-list.md`, `friendships-outgoing-list.md`, `followers-list.md`, `subscribes-list.md`; пусто или неизвестный пользователь → `200` с пустым списком. yaml не меняется в этой части. | fix.md в.4, 2026-11-19 | действует |
| Р1-5 | Порядок: заявки и друзья — `createdAt DESC, id DESC`; порядок записывается в `*.md`. | fix.md в.5, 2026-11-19 | действует |
| Р1-6 | `FriendshipController` добавляется в `assignableTypes` существующего `FriendshipOfferExceptionHandler`, отдельный обработчик не заводим. | fix.md в.6, 2026-11-19 | действует |
| Р1-7 | Три use case'а: `GetIncomingFriendshipOffers`, `GetOutgoingFriendshipOffers`, `GetFriends`. | fix.md в.7, 2026-11-19 | действует |
| Р1-8 | Элемент заявки — существующий `FriendshipOfferResult`; для друзей — новый `FriendResult(id, userId)`. | fix.md в.8, 2026-11-19 | действует |
| Р1-9 | Тесты: один E2E-класс (наследник `E2ETest`): happy-path и пустой ответ на каждую операцию + кейс `400` на невалидный параметр. Уточнение после Р1-1…Р1-3 — раунд 2. | fix.md в.9, 2026-11-19 | заменено Р2-7 |
| Р1-10 | Код пишет Claude по пунктам плана, по одному, по команде; пользователь проверяет. | fix.md в.10, 2026-11-19 | действует |
| Р1-11 | Ручные тесты: `manual-test-curl.md` + коллекция Bruno (по процессу; возражений в fix.md нет). | fix.md «К сведению», 2026-11-19 | действует |
| Р2-1 | Обёртки: `incoming`/`outgoing` — `{offers, next}` (как `GET /friendship-offer`), `friends` — `{friends, next}`. | fix.md р.2 в.1, 2026-11-19 | действует |
| Р2-2 | Параметры страницы как в `GET /friendship-offer`: `limit` 1..100 (по умолчанию 20); `afterCreatedAt` + `afterId` только вместе (одна часть → `400 VALIDATION_ERROR`); `next: null` — конец. | fix.md р.2 в.2, 2026-11-19 | действует |
| Р2-3 | `status` в `incoming`/`outgoing` — необязательный, одно значение `PENDING`/`ACCEPTED`/`REJECTED` (статус заявки), по умолчанию `PENDING`; неизвестное → `400`. | fix.md р.2 в.3, 2026-11-19 | действует |
| Р2-4 | Курсор `friends` — `createdAt` и `id` дружбы, порядок `createdAt DESC, id DESC`; только `status = ACTIVE`. | fix.md р.2 в.4, 2026-11-19 | действует |
| Р2-5 | Новый Liquibase-changeset `011-…`: индексы `friendships` заменить на `(first_user_id, created_at DESC, id DESC)` и `(second_user_id, created_at DESC, id DESC)`; запрос друзей — `UNION ALL` двух веток. | fix.md р.2 в.5, 2026-11-19 | действует |
| Р2-6 | Порт: `FriendshipOfferRepository` — `findIncomingPage`/`findIncomingPageAfter`, `findOutgoingPage`/`findOutgoingPageAfter` с переиспользованием `FriendshipOfferPageResponse`, `FriendshipOfferCursor`, `FriendshipOfferPageResult`; `FriendshipsRepository` — новые `FriendPageResponse`/`FriendCursor`/`FriendPageResult`. | fix.md р.2 в.6, 2026-11-19 | действует |
| Р2-7 | Тесты: один E2E-класс — на каждую операцию первая страница и пустой ответ; `incoming` — вторая страница по `next` и `status=ACCEPTED`; `incoming` без `X-User-Id` → `401`; `friends` с невалидным `userId` → `400`. | fix.md р.2 в.7, 2026-11-19 | действует |
| Р2-8 | Курсор для `followers`/`subscribes` — отдельная задача 40002, в 40000 не делается. | fix.md р.2 в.8, 2026-11-19 | действует |
| Р2-9 | В примере `found` в `friends-get.yaml` оставить только C (`…4e02`). | fix.md р.2 в.9, 2026-11-19 | действует |
| Р2-10 | Миграции — Liquibase (`db/changes/NNN-….sql`), не Flyway. | fix.md р.2 «К сведению», 2026-11-19 | действует |
