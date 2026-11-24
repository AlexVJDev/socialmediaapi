# 40000 — Операции контракта без реализации. План

Режим Ментора: ДА. Эталон — `decisions.md`; при расхождении плана с решением прав `decisions.md`.
Пункты выполняет Claude по одному, по команде пользователя; пользователь проверяет (Р1-10).

Суть: `incoming`/`outgoing` (заявки текущего пользователя из `X-User-Id`, фильтр `status`) и `friends` (друзья `userId`)
отдают страницу по курсору, как `GET /friendship-offer` и лента (Р1-1, Р1-3, Р2-1…Р2-4).
Контракт меняется до кода: из него генерируются интерфейсы `FriendshipOfferApi`/`FriendshipApi` и DTO.

| Итого | Новые | Изменить |
|---|---|---|
| файлов | ~27 (6 схем, changeset, 15 Java, E2E-тест, 2 ручных теста + коллекция Bruno) | ~22 (4 yaml путей, модуль, 5 `*.md`, `pom.xml`, changelog, 2 порта, 2 адаптера, 2 маппера, контроллер, обработчик) |

Не меняются: `docs/api/schemas/friendship-offer.yaml`, create/accept/reject (задача 40001), `followers`/`subscribes` (задача 40002, Р2-8),
domain-слой.

## Где будут файлы

```
docs/api/
├── openapi.yaml                                       (без изменений: пути уже есть)
├── modules/friendships.yaml                           (изменить) securitySchemes.userIdHeader
├── paths/
│   ├── friendship-offers-get.yaml                     (изменить) ответ → $ref friendship-offer-page.yaml
│   ├── friendships-incoming-get.yaml                  (изменить) X-User-Id, status, курсор, {offers, next}, 401
│   ├── friendships-outgoing-get.yaml                  (изменить) так же
│   └── friends-get.yaml                               (изменить) курсор, {friends, next}, пример только C
├── schemas/
│   ├── friendship-offer-page.yaml                     (новый) {offers, next}
│   ├── friendship-offer-cursor.yaml                   (новый) {createdAt, id}, nullable
│   ├── friendship-offer-status.yaml                   (новый) enum для параметра status
│   ├── friend.yaml                                    (новый) {id, userId}
│   ├── friend-page.yaml                               (новый) {friends, next}
│   └── friend-cursor.yaml                             (новый) {createdAt, id}, nullable
└── friends-list.md, friendships-incoming-list.md, friendships-outgoing-list.md,
    followers-list.md, subscribes-list.md              (изменить) без 404, порядок, курсор, status

pom.xml                                                (изменить) openapi.friendships.* — имена и импорты моделей

src/main/resources/db/
├── db.changelog-master.yaml                           (изменить) include 011
└── changes/011-friendships-page-indexes.sql           (новый) индексы (user, created_at DESC, id DESC)

src/main/java/ru/job4j/socialmediaapi/friendships/internal/
├── application/
│   ├── exception/IncompleteFriendCursorException.java (новый)
│   ├── mapper/FriendshipApplicationMapper.java        (изменить) страница друзей → результат
│   ├── model/
│   │   ├── GetUserFriendshipOffersCommand.java        (новый) userId, status, курсор, limit
│   │   ├── GetFriendsCommand.java                     (новый) userId, курсор, limit
│   │   ├── FriendResult.java                          (новый) id, userId
│   │   ├── FriendCursorResult.java                    (новый)
│   │   └── FriendPageResult.java                      (новый)
│   ├── port/
│   │   ├── FriendshipOfferRepository.java             (изменить) find{Incoming,Outgoing}Page[After]
│   │   ├── FriendshipsRepository.java                 (изменить) findFriendsPage[After]
│   │   └── model/
│   │       ├── FindFriendResponse.java                (новый) id, userId
│   │       ├── FriendCursor.java                      (новый)
│   │       └── FriendPageResponse.java                (новый)
│   └── usecase/
│       ├── GetIncomingFriendshipOffers.java           (новый)
│       ├── GetOutgoingFriendshipOffers.java           (новый)
│       └── GetFriends.java                            (новый)
└── infrastructure/
    ├── persistence/adapter/
    │   ├── FriendshipOfferPersistenceAdapter.java     (изменить) 4 SQL
    │   └── FriendshipsPersistenceAdapter.java         (изменить) 2 SQL с UNION ALL
    └── web/
        ├── FriendshipOfferController.java             (изменить) incoming/outgoing вместо 501
        ├── FriendshipController.java                  (новый) implements FriendshipApi
        ├── FriendshipOfferExceptionHandler.java       (изменить) assignableTypes + новое исключение
        └── mapper/
            ├── FriendshipOfferWebMapper.java          (изменить) параметры → команда
            └── FriendshipWebMapper.java               (новый)

src/test/java/ru/job4j/socialmediaapi/friendships/internal/infrastructure/web/
└── FriendshipListsE2ETest.java                        (новый)

docs/md/task/40000/manual-test-curl.md, manual-test-bruno.md   (новые)
bruno/40000/*.bru                                              (новые)
```

## Файлы

| № | Файл | Слой | Действие | Решение |
|---|---|---|---|---|
| 1 | `schemas/friendship-offer-{page,cursor}.yaml`, `paths/friendship-offers-get.yaml` | контракт | вынести схему страницы заявок | Р2-1 |
| 2 | `schemas/friendship-offer-status.yaml`, `paths/friendships-{incoming,outgoing}-get.yaml`, `modules/friendships.yaml` | контракт | изменить операции | Р1-3, Р2-1…Р2-3 |
| 3 | `schemas/friend{,-page,-cursor}.yaml`, `paths/friends-get.yaml` | контракт | изменить операцию | Р1-1, Р2-1, Р2-4, Р2-9 |
| 4 | `pom.xml` + заглушки в `FriendshipOfferController`, новый `FriendshipController` (заглушка) | сборка | имена моделей; сборка зелёная | Р2-1 |
| 5 | 5 × `docs/api/*.md` | документация | изменить | Р1-4, Р1-5, Р2-1…Р2-4 |
| 6 | `changes/011-friendships-page-indexes.sql`, `db.changelog-master.yaml` | БД | индексы | Р2-5, Р2-10 |
| 7 | `FriendshipOfferRepository`, `FriendshipOfferPersistenceAdapter` | порт + адаптер | 4 метода | Р2-6, Р2-3, Р1-5 |
| 8 | `GetUserFriendshipOffersCommand`, `GetIncoming…`, `GetOutgoing…` | application | use case'ы | Р1-7, Р1-8, Р2-2 |
| 9 | `FriendshipOfferWebMapper`, `FriendshipOfferController`, `FriendshipOfferExceptionHandler` | web | incoming/outgoing | Р1-3, Р1-6 |
| 10 | `FindFriendResponse`, `FriendCursor`, `FriendPageResponse`, `FriendshipsRepository`, `FriendshipsPersistenceAdapter` | порт + адаптер | страница друзей | Р2-4…Р2-6 |
| 11 | `GetFriendsCommand`, `FriendResult`, `FriendCursorResult`, `FriendPageResult`, `IncompleteFriendCursorException`, `FriendshipApplicationMapper`, `GetFriends` | application | use case | Р1-7, Р1-8, Р2-2 |
| 12 | `FriendshipWebMapper`, `FriendshipController`, `FriendshipOfferExceptionHandler` | web | friends | Р1-6 |
| 13 | `FriendshipListsE2ETest` | тест | новый | Р2-7 |
| 14 | `manual-test-curl.md`, `manual-test-bruno.md`, `bruno/40000/` | ручные тесты | новые | Р1-11 |

Подзадачи: **A** — п. 1–5 (контракт, сборка зелёная); **B** — п. 6–9 (заявки); **C** — п. 10–12 (друзья); **D** — п. 13–14 (тесты).

---

## Подзадача A. Контракт

## 1. Схема страницы заявок (Р2-1)
**Зачем:** `incoming`/`outgoing` отдают ту же страницу, что `GET /friendship-offer`; одна схема → один DTO `FriendshipOfferPageWebResponse`.

- `schemas/friendship-offer-cursor.yaml` — как `post-cursor.yaml`: `nullable: true`, `required: [createdAt, id]`, описание «позиция последней заявки; `null` — заявок больше нет».
- `schemas/friendship-offer-page.yaml` — как `post-page.yaml`: `required: [offers, next]`, `offers` — массив `$ref: './friendship-offer.yaml'`, `next` — `$ref: './friendship-offer-cursor.yaml'`.
- `paths/friendship-offers-get.yaml` — inline-схему ответа `200` заменить на `$ref: '../schemas/friendship-offer-page.yaml'`. Форма JSON не меняется.

## 2. `incoming` / `outgoing` (Р1-3, Р2-1…Р2-3, Р1-5)
**Зачем:** заявки текущего пользователя, страницей, с фильтром по статусу заявки.

- `schemas/friendship-offer-status.yaml` — `type: string`, `enum: [PENDING, ACCEPTED, REJECTED]`. В `friendship-offer.yaml` не подключаем
  (иначе меняется тип поля `status` в существующем DTO).
- `paths/friendships-incoming-get.yaml`:
  - `security: - userIdHeader: []`; `description` — «получатель — текущий пользователь (X-User-Id); порядок `createdAt DESC, id DESC`; курсор как у `GET /friendship-offer`»;
  - параметр `userId` удалить;
  - параметры: `status` (`$ref` на схему статуса, `default: PENDING`), `limit`, `afterCreatedAt`, `afterId` — копия из `friendship-offers-get.yaml`
    (без `example` у `afterCreatedAt`, см. 41226 Р-12);
  - `200` → `$ref: '../schemas/friendship-offer-page.yaml'`, примеры `found` (две заявки B из `sql-examples-swagger.sql`, `next: null`) и `empty` (`{offers: [], next: null}`);
  - `400` — описание: `status` не из списка, `limit` вне 1..100, `afterCreatedAt`/`afterId` некорректны или задан только один;
  - `401` → `$ref: '../responses/unauthorized.yaml'`.
- `paths/friendships-outgoing-get.yaml` — так же, «отправитель — текущий пользователь», примеры — заявки A.
- `modules/friendships.yaml` — блок `components.securitySchemes.userIdHeader`, как в `modules/posts.yaml` (иначе генератор не знает схему).

## 3. `friends` (Р1-1, Р2-1, Р2-4, Р2-9)
**Зачем:** публичный список друзей `userId`, страницей.

- `schemas/friend.yaml` — `required: [id, userId]`, `id` — id дружбы, `userId` — другой пользователь пары (перенести из inline).
- `schemas/friend-cursor.yaml` — как `friendship-offer-cursor.yaml` («позиция последней дружбы»).
- `schemas/friend-page.yaml` — `required: [friends, next]`.
- `paths/friends-get.yaml`: `userId` в query остаётся; добавить `limit`/`afterCreatedAt`/`afterId`; `200` → `friend-page.yaml`;
  пример `found` — только C (`…4e02`, `userId: 7d1f0a52…`), `next: null`; `400` — описание как в п. 2.

## 4. `pom.xml` и заглушки — сборка зелёная (Р2-1)
**Зачем:** генератор должен дать понятные имена новым моделям; изменившиеся сигнатуры интерфейсов должны компилироваться.

- `openapi.friendships.inline-names`: убрать `getFriendshipOffers_200_response*` и `getFriends_200_response_inner` (схемы теперь в файлах).
- `openapi.friendships.model-names`: добавить `friendship-offer-page=FriendshipOfferPageWebResponse`, `friendship-offer-cursor=FriendshipOfferCursorWebResponse`,
  `friendship-offer-status=FriendshipOfferStatusWebRequest`, `friend=FriendWebResponse`, `friend-page=FriendPageWebResponse`, `friend-cursor=FriendCursorWebResponse`.
- `openapi.friendships.responses`: имена файлов `friendship-offer-page,friendship-offer-cursor,friend,friend-page,friend-cursor`;
  `openapi.friendships.requests`: `friendship-offer-status`; `openapi.friendships.imports` — новые классы.
- `./mvnw -q compile`: сигнатуры `getIncomingFriendshipOffers(status, limit, afterCreatedAt, afterId)` и т.п. поменяются —
  в `FriendshipOfferController` поправить заглушки (по-прежнему 501), создать `FriendshipController implements FriendshipApi` с заглушкой 501.
- Проверить, что `status` сгенерирован как enum (неизвестное значение → `MethodArgumentTypeMismatchException` → уже `400`). Если генератор
  оставит `String` — вопрос пользователю, не обходить молча.

## 5. Описания `docs/api/*.md` (Р1-4, Р1-5)
- `friendships-incoming-list.md`, `friendships-outgoing-list.md`: `X-User-Id` вместо `userId`, `status` (по умолчанию `PENDING`), курсор, ответ `{offers, next}`, порядок, `401`; убрать `404`.
- `friends-list.md`: курсор, `{friends, next}`, порядок; убрать `404`.
- `followers-list.md`, `subscribes-list.md`: только убрать `404` и написать «неизвестный пользователь → пустой список».

---

## Подзадача B. Заявки: `incoming` / `outgoing`

## 6. Индексы друзей (Р2-5, Р2-10)
**Зачем:** страница друзей читается по `(user, created_at DESC, id DESC)` без сортировки в памяти.

- `db/changes/011-friendships-page-indexes.sql` — формат как `010-…`: `DROP INDEX idx_friendships_first_user`, `…_second_user`;
  `CREATE INDEX idx_friendships_first_user_page ON friendships (first_user_id, created_at DESC, id DESC)`, то же для `second_user_id`;
  `--rollback` — обратно.
- `db.changelog-master.yaml` — `include` перед блоком `dev/swagger-examples.sql`.

## 7. Порт и адаптер заявок (Р2-6, Р2-3, Р1-5)
- `FriendshipOfferRepository`: `findIncomingPage(UUID toUserId, FriendshipOfferStatus status, int limit)`,
  `findIncomingPageAfter(UUID toUserId, FriendshipOfferStatus status, FriendshipOfferCursor after, int limit)`, то же `Outgoing` с `fromUserId`.
  Возврат — `FriendshipOfferPageResponse`; Javadoc — по образцу `findPage`/`findPageAfter`.
- `FriendshipOfferPersistenceAdapter`: 4 SQL по образцу `FIND_FIRST_PAGE`/`FIND_PAGE_AFTER` + `WHERE to_user_id = ? AND status = ?`
  (`from_user_id` для outgoing) — ложится на `idx_friendship_offer_incoming/outgoing`; переиспользовать приватный `findPage(limit, sql, args)` (`limit + 1` → `next`).

## 8. Use case'ы заявок (Р1-7, Р1-8, Р2-2)
- `GetUserFriendshipOffersCommand(UUID userId, FriendshipOfferStatus status, Instant afterCreatedAt, UUID afterId, int limit)` — общая для двух use case'ов.
- `GetIncomingFriendshipOffers`, `GetOutgoingFriendshipOffers` — по образцу `GetFriendshipOffers`: проверка полноты курсора
  (`IncompleteFriendshipOfferCursorException`), первая страница / после курсора, `mapper.toResult(...)` → `FriendshipOfferPageResult`.

## 9. Web: `incoming` / `outgoing` (Р1-3, Р1-6)
- `FriendshipOfferWebMapper`: `toUserOffersCommand(UUID userId, FriendshipOfferStatusWebRequest status, Integer limit, Instant afterCreatedAt, UUID afterId)`;
  ответ — существующий `toResponse(FriendshipOfferPageResult)`.
- `FriendshipOfferController`: зависимость `CurrentUserProvider` (как `FeedController`); заглушки 501 заменить:
  `userId = currentUser.currentUserId()` → use case → `200`. Javadoc как у `getFriendshipOffers` + «нет/не UUID `X-User-Id` → `401` (`GlobalExceptionHandler`)».
- `FriendshipOfferExceptionHandler` — без изменений в этом пункте (все ошибки уже покрыты).

---

## Подзадача C. Друзья: `friends`

## 10. Порт и адаптер друзей (Р2-4…Р2-6)
- `port/model`: `FindFriendResponse(UUID id, UUID userId)`, `FriendCursor(Instant createdAt, UUID id)`, `FriendPageResponse(List<FindFriendResponse> friends, Optional<FriendCursor> next)`.
- `FriendshipsRepository`: `findFriendsPage(UUID userId, int limit)`, `findFriendsPageAfter(UUID userId, FriendCursor after, int limit)`, `MAX_LIMIT`.
- `FriendshipsPersistenceAdapter`: SQL —
  ```sql
  SELECT id, second_user_id AS friend_id, created_at FROM friendships
   WHERE first_user_id = ? AND status = 'ACTIVE' [& (created_at, id) < (?, ?)]
  UNION ALL
  SELECT id, first_user_id, created_at FROM friendships
   WHERE second_user_id = ? AND status = 'ACTIVE' [& (created_at, id) < (?, ?)]
  ORDER BY created_at DESC, id DESC
  LIMIT ?
  ```
  `limit + 1` → `next` по последней строке страницы (как у заявок).

## 11. Use case друзей (Р1-7, Р1-8, Р2-2)
- `model`: `GetFriendsCommand(UUID userId, Instant afterCreatedAt, UUID afterId, int limit)`, `FriendResult(UUID id, UUID userId)`,
  `FriendCursorResult(Instant createdAt, UUID id)`, `FriendPageResult(List<FriendResult> friends, FriendCursorResult next)`.
- `exception/IncompleteFriendCursorException` — по образцу `IncompleteFriendshipOfferCursorException`.
- `FriendshipApplicationMapper`: `FriendPageResult toResult(FriendPageResponse)` + `unwrapNext` как в `FriendshipOfferApplicationMapper`.
- `GetFriends` — по образцу `GetFriendshipOffers`.

## 12. Web: `friends` (Р1-6)
- `FriendshipWebMapper` (MapStruct): `toCommand(UUID userId, Integer limit, Instant afterCreatedAt, UUID afterId)`, `FriendPageWebResponse toResponse(FriendPageResult)`.
- `FriendshipController implements FriendshipApi` — заглушку заменить: use case → `200`.
- `FriendshipOfferExceptionHandler`: `assignableTypes = {FriendshipOfferController.class, FriendshipController.class}`,
  в список `400` добавить `IncompleteFriendCursorException`; Javadoc класса — «обе контроллера модуля».

---

## Подзадача D. Тесты

## 13. `FriendshipListsE2ETest` (Р2-7)
`@E2ETest`, случайные пользователи на каждый тест (как `FriendshipOfferE2ETest`); заявки создаются и принимаются через API.
- `incoming`: первая страница (две заявки, порядок), вторая страница по `next` (`limit=1`), `status=ACCEPTED`, пусто → `{offers: [], next: null}`, без `X-User-Id` → `401`.
- `outgoing`: первая страница, пусто.
- `friends`: друг после accept (`id` дружбы, `userId` — другой), пусто, `userId=abc` → `400 VALIDATION_ERROR`.

После пункта: `./mvnw test` (нужен Docker). Напоминание: `ApplicationModulesTest` — по команде пользователя.

## 14. Ручные тесты (Р1-11)
- `manual-test-curl.md` — скрипт sh: создать заявки A→B, C→B, принять одну, вызвать incoming/outgoing/friends, вторую страницу, 401/400; итог OK/FAIL.
- `manual-test-bruno.md` — по каждому запросу: curl (`{{baseUrl}}`, токен `{{collection_dev_token}}`, `X-User-Id`), метод, JSON, ожидаемый ответ.
- `bruno/40000/` — коллекция по образцу `bruno/41225/`.
