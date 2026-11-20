# 40000 — журнал решений

| ID | Решение (1–2 строки) | Источник | Статус |
|---|---|---|---|
| Р1-1 | Все три списка — страница с курсором вместо массива: параметры `limit`/`afterCreatedAt`/`afterId`, ответ — обёртка с полем `next` (контракт меняется). Детали — раунд 2. | fix.md в.1, 2026-11-19 | действует |
| Р1-2 | `incoming`/`outgoing` — в контракт добавляется query-параметр `status`. Детали (по умолчанию, допустимые значения) — раунд 2. | fix.md в.2, 2026-11-19 | заменено Р3-2 |
| Р1-3 | `incoming`/`outgoing` — текущий пользователь из `CurrentUserProvider` (`X-User-Id`), query `userId` убирается из контракта; `friends` — `userId` в query (публичный список). Перевод create/accept/reject на `X-User-Id` — отдельная задача 40001. | чат 2026-11-19 | заменено Р3-2 (incoming/outgoing), friends — действует |
| Р1-4 | `404 «Пользователь не найден»` убирается из `friends-list.md`, `friendships-incoming-list.md`, `friendships-outgoing-list.md`, `followers-list.md`, `subscribes-list.md`; пусто или неизвестный пользователь → `200` с пустым списком. yaml не меняется в этой части. | fix.md в.4, 2026-11-19 | заменено Р4-5 (incoming/outgoing), остальные *.md — действует |
| Р1-5 | Порядок: заявки и друзья — `createdAt DESC, id DESC`; порядок записывается в `*.md`. | fix.md в.5, 2026-11-19 | действует |
| Р1-6 | `FriendshipController` добавляется в `assignableTypes` существующего `FriendshipOfferExceptionHandler`, отдельный обработчик не заводим. | fix.md в.6, 2026-11-19 | действует |
| Р1-7 | Три use case'а: `GetIncomingFriendshipOffers`, `GetOutgoingFriendshipOffers`, `GetFriends`. | fix.md в.7, 2026-11-19 | заменено Р3-3 |
| Р1-8 | Элемент заявки — существующий `FriendshipOfferResult`; для друзей — новый `FriendResult(id, userId)`. | fix.md в.8, 2026-11-19 | действует |
| Р1-9 | Тесты: один E2E-класс (наследник `E2ETest`): happy-path и пустой ответ на каждую операцию + кейс `400` на невалидный параметр. Уточнение после Р1-1…Р1-3 — раунд 2. | fix.md в.9, 2026-11-19 | заменено Р2-7 |
| Р1-10 | Код пишет Claude по пунктам плана, по одному, по команде; пользователь проверяет. | fix.md в.10, 2026-11-19 | заменено Р5-1 |
| Р1-11 | Ручные тесты: `manual-test-curl.md` + коллекция Bruno (по процессу; возражений в fix.md нет). | fix.md «К сведению», 2026-11-19 | действует |
| Р2-1 | Обёртки: `incoming`/`outgoing` — `{offers, next}` (как `GET /friendship-offer`), `friends` — `{friends, next}`. | fix.md р.2 в.1, 2026-11-19 | заменено Р3-3 (incoming/outgoing), friends — действует |
| Р2-2 | Параметры страницы как в `GET /friendship-offer`: `limit` 1..100 (по умолчанию 20); `afterCreatedAt` + `afterId` только вместе (одна часть → `400 VALIDATION_ERROR`); `next: null` — конец. | fix.md р.2 в.2, 2026-11-19 | действует |
| Р2-3 | `status` в `incoming`/`outgoing` — необязательный, одно значение `PENDING`/`ACCEPTED`/`REJECTED` (статус заявки), по умолчанию `PENDING`; неизвестное → `400`. | fix.md р.2 в.3, 2026-11-19 | заменено Р4-2 |
| Р2-4 | Курсор `friends` — `createdAt` и `id` дружбы, порядок `createdAt DESC, id DESC`; только `status = ACTIVE`. | fix.md р.2 в.4, 2026-11-19 | действует |
| Р2-5 | Новый Liquibase-changeset `011-…`: индексы `friendships` заменить на `(first_user_id, created_at DESC, id DESC)` и `(second_user_id, created_at DESC, id DESC)`; запрос друзей — `UNION ALL` двух веток. | fix.md р.2 в.5, 2026-11-19 | действует |
| Р2-6 | Порт: `FriendshipOfferRepository` — `findIncomingPage`/`findIncomingPageAfter`, `findOutgoingPage`/`findOutgoingPageAfter` с переиспользованием `FriendshipOfferPageResponse`, `FriendshipOfferCursor`, `FriendshipOfferPageResult`; `FriendshipsRepository` — новые `FriendPageResponse`/`FriendCursor`/`FriendPageResult`. | fix.md р.2 в.6, 2026-11-19 | заменено Р4-3 (заявки), друзья — действует |
| Р2-7 | Тесты: один E2E-класс — на каждую операцию первая страница и пустой ответ; `incoming` — вторая страница по `next` и `status=ACCEPTED`; `incoming` без `X-User-Id` → `401`; `friends` с невалидным `userId` → `400`. | fix.md р.2 в.7, 2026-11-19 | заменено Р4-6 |
| Р2-8 | Курсор для `followers`/`subscribes` — отдельная задача 40002, в 40000 не делается. | fix.md р.2 в.8, 2026-11-19 | действует |
| Р2-9 | В примере `found` в `friends-get.yaml` оставить только C (`…4e02`). | fix.md р.2 в.9, 2026-11-19 | действует |
| Р2-10 | Миграции — Liquibase (`db/changes/NNN-….sql`), не Flyway. | fix.md р.2 «К сведению», 2026-11-19 | действует |
| Р3-1 | Ни в одном репозитории/адаптере/сервисе нет запросов по всей таблице: каждый запрос ограничен `WHERE` по ключу/владельцу; только курсор или `LIMIT` — не ограничение. Правило общее — записано в `CLAUDE2.md`. | чат 2026-11-20 | действует |
| Р3-2 | `GET /friendship-offer` остаётся и ограничивается текущим пользователем (`X-User-Id`, нет → `401`); новый обязательный query `direction` (входящие/исходящие), нет или неизвестное → `400`. Детали — раунд 4. | чат 2026-11-20 | действует |
| Р3-3 | `GET /friendship-offer/incoming` и `/outgoing` удаляются из контракта и кода; заявки читаются только через `GET /friendship-offer`. | чат 2026-11-20 | действует |
| Р4-1 | `direction` — enum `INCOMING`/`OUTGOING` (схема `friendship-offer-direction.yaml`); неизвестное → `400 VALIDATION_ERROR`. | fix.md р.4 в.1, 2026-11-20 | действует |
| Р4-2 | `status` в `GET /friendship-offer` — необязательный, одно значение `PENDING`/`ACCEPTED`/`REJECTED`, по умолчанию `PENDING`; неизвестное → `400`. | fix.md р.4 в.2, 2026-11-20 | действует |
| Р4-3 | Расширяются существующие `GetFriendshipOffers`/`GetFriendshipOffersCommand` и `findPage`/`findPageAfter` (+ `userId`, `direction`, `status`); адаптер выбирает SQL по `direction`; enum `FriendshipOfferDirection` — `application/port/model`. | fix.md р.4 в.3, 2026-11-20 | действует |
| Р4-4 | Changeset `011`: индексы заявок → `(to_user_id\|from_user_id, status, created_at DESC, id DESC)`, `idx_friendship_offer_created` удаляется; вместе с индексами `friendships` (Р2-5). | fix.md р.4 в.4, 2026-11-20 | действует |
| Р4-5 | `friendships-incoming-list.md`, `friendships-outgoing-list.md` удаляются; новый `docs/api/friendship-offers-list.md`. | fix.md р.4 в.5, 2026-11-20 | действует |
| Р4-6 | Тесты: существующие тесты страницы заявок (адаптер, use case, контроллер, `FriendshipOfferE2ETest.GetPage`, web-маппер) переделываются под `userId`/`direction`/`status` + без `direction` → `400`, без `X-User-Id` → `401`, `status=ACCEPTED`; тесты `incoming`/`outgoing` удаляются; новый E2E — только `friends` (первая страница, пусто, `userId=abc` → `400`). | fix.md р.4 в.6, 2026-11-20 | действует |
| Р5-1 | План v2 принят; Claude выполняет все пункты подряд без остановок, вопросы — только в конце. | чат 2026-11-20 | действует |
| Р6-1 | `FriendshipOfferPersistenceAdapter.findPage`/`findPageAfter` — явный `Objects.requireNonNull(direction, "direction")`. | code-review.md п.1, 2026-11-20 | действует |
| Р6-2 | `400` вместо `401` при отсутствии и параметров, и `X-User-Id` — оставить до Keycloak; описать в таблице ошибок `friendship-offers-list.md`. | code-review.md п.2, 2026-11-20 | действует |
| Р6-3 | Javadoc и комментарии в Java-файлах задачи 40000 (новых и изменённых, main и test) — на русском; остальной проект — отдельной задачей. | code-review.md п.3, 2026-11-20 | действует |
