# 40000 — Операции контракта без реализации. План (v2)

Режим Ментора: ДА. Эталон — `decisions.md`; при расхождении плана с решением прав `decisions.md`.
Claude выполняет все пункты подряд, вопросы — в конце (Р5-1).
Предыдущий план — `archive/task-manual-v1.md` (не использовать).

Суть:
- `GET /friendship-offer` — заявки **текущего пользователя** (`X-User-Id`), обязательный `direction` = `INCOMING`/`OUTGOING`,
  `status` (по умолчанию `PENDING`), курсор как сейчас (Р3-2, Р4-1, Р4-2). Ни одного запроса по всей таблице (Р3-1).
- `GET /friendship-offer/incoming` и `/outgoing` удаляются из контракта и кода (Р3-3).
- `GET /friendships?userId=` — публичный список друзей страницей `{friends, next}` (Р1-1, Р1-3, Р2-1, Р2-4).

| Итого | Новые | Изменить | Удалить |
|---|---|---|---|
| файлов | ~23 (7 схем, changeset, `friendship-offers-list.md`, 11 Java, E2E-тест, 2 ручных теста + Bruno) | ~24 | 4 (2 yaml путей, 2 `*.md`) |

Не меняются: `docs/api/schemas/friendship-offer.yaml`, create/accept/reject (задача 40001), `followers`/`subscribes` (40002, Р2-8), domain-слой.

## Где будут файлы

```
docs/api/
├── openapi.yaml                                       (изменить) убрать пути incoming/outgoing
├── modules/friendships.yaml                           (изменить) убрать incoming/outgoing; securitySchemes.userIdHeader
├── paths/
│   ├── friendship-offers-get.yaml                     (изменить) X-User-Id, direction, status, $ref страницы, 401
│   ├── friendships-incoming-get.yaml                  (удалить)
│   ├── friendships-outgoing-get.yaml                  (удалить)
│   └── friends-get.yaml                               (изменить) курсор, {friends, next}, пример только C
├── schemas/
│   ├── friendship-offer-page.yaml                     (новый) {offers, next}
│   ├── friendship-offer-cursor.yaml                   (новый) {createdAt, id}, nullable
│   ├── friendship-offer-direction.yaml                (новый) enum INCOMING/OUTGOING
│   ├── friendship-offer-status.yaml                   (новый) enum PENDING/ACCEPTED/REJECTED
│   ├── friend.yaml                                    (новый) {id, userId}
│   ├── friend-page.yaml                               (новый) {friends, next}
│   └── friend-cursor.yaml                             (новый) {createdAt, id}, nullable
├── friendship-offers-list.md                          (новый)
├── friendships-incoming-list.md, friendships-outgoing-list.md   (удалить)
└── friends-list.md, followers-list.md, subscribes-list.md       (изменить) без 404; friends — курсор, порядок

pom.xml                                                (изменить) openapi.friendships.* — имена и импорты моделей

src/main/resources/db/
├── db.changelog-master.yaml                           (изменить) include 011
└── changes/011-friendships-page-indexes.sql           (новый) индексы заявок и дружб

src/main/java/ru/job4j/socialmediaapi/friendships/internal/
├── application/
│   ├── exception/IncompleteFriendCursorException.java (новый)
│   ├── mapper/FriendshipApplicationMapper.java        (изменить) страница друзей → результат
│   ├── model/
│   │   ├── GetFriendshipOffersCommand.java            (изменить) + userId, direction, status
│   │   ├── GetFriendsCommand.java                     (новый)
│   │   ├── FriendResult.java                          (новый)
│   │   ├── FriendCursorResult.java                    (новый)
│   │   └── FriendPageResult.java                      (новый)
│   ├── port/
│   │   ├── FriendshipOfferRepository.java             (изменить) findPage/findPageAfter + userId, direction, status
│   │   ├── FriendshipsRepository.java                 (изменить) findFriendsPage[After]
│   │   └── model/
│   │       ├── FriendshipOfferDirection.java          (новый) INCOMING, OUTGOING
│   │       ├── FindFriendResponse.java                (новый)
│   │       ├── FriendCursor.java                      (новый)
│   │       └── FriendPageResponse.java                (новый)
│   └── usecase/
│       ├── GetFriendshipOffers.java                   (изменить) передаёт фильтр в порт
│       └── GetFriends.java                            (новый)
└── infrastructure/
    ├── persistence/adapter/
    │   ├── FriendshipOfferPersistenceAdapter.java     (изменить) 4 SQL вместо 2, выбор по direction
    │   └── FriendshipsPersistenceAdapter.java         (изменить) 2 SQL с UNION ALL
    └── web/
        ├── FriendshipOfferController.java             (изменить) CurrentUserProvider; удалить incoming/outgoing
        ├── FriendshipController.java                  (новый) implements FriendshipApi
        ├── FriendshipOfferExceptionHandler.java       (изменить) assignableTypes + IncompleteFriendCursorException
        └── mapper/
            ├── FriendshipOfferWebMapper.java          (изменить) toGetPageCommand + userId, direction, status
            └── FriendshipWebMapper.java               (новый)

src/test/java/ru/job4j/socialmediaapi/friendships/internal/
├── application/usecase/GetFriendshipOffersTest.java                     (изменить)
├── infrastructure/persistence/adapter/FriendshipOfferPersistenceAdapterTest.java (изменить)
├── infrastructure/web/FriendshipOfferGetControllerTest.java             (изменить)
├── infrastructure/web/FriendshipOfferE2ETest.java                       (изменить) GetPage; удалить IncomingOutgoing
├── infrastructure/web/mapper/FriendshipOfferWebMapperTest.java          (изменить)
└── infrastructure/web/FriendshipE2ETest.java                            (новый) friends

docs/md/task/40000/manual-test-curl.md, manual-test-bruno.md   (новые)
bruno/40000/*.bru                                              (новые)
```

## Файлы

| № | Файл | Слой | Действие | Решение |
|---|---|---|---|---|
| 1 | 4 схемы заявок, `paths/friendship-offers-get.yaml`, удалить `friendships-{incoming,outgoing}-get.yaml`, `openapi.yaml`, `modules/friendships.yaml` | контракт | `GET /friendship-offer` | Р3-2, Р3-3, Р4-1, Р4-2 |
| 2 | 3 схемы друзей, `paths/friends-get.yaml` | контракт | `friends` | Р1-1, Р2-1, Р2-4, Р2-9 |
| 3 | `pom.xml` | сборка | имена моделей | Р2-1 |
| 4 | `friendship-offers-list.md` (новый), удалить 2 `*.md`, `friends-list.md`, `followers-list.md`, `subscribes-list.md` | документация | описания | Р1-4, Р1-5, Р4-5 |
| 5 | `changes/011-friendships-page-indexes.sql`, `db.changelog-master.yaml` | БД | индексы | Р2-5, Р4-4, Р2-10 |
| 6 | `FriendshipOfferDirection`, `FriendshipOfferRepository`, `FriendshipOfferPersistenceAdapter` | порт + адаптер | фильтр по пользователю | Р3-1, Р4-3, Р1-5 |
| 7 | `GetFriendshipOffersCommand`, `GetFriendshipOffers` | application | фильтр в use case | Р4-2, Р4-3 |
| 8 | `FriendshipOfferWebMapper`, `FriendshipOfferController` | web | `X-User-Id`, удалить incoming/outgoing | Р3-2, Р3-3 |
| 9 | 5 тестовых классов заявок | тесты | переделать | Р4-6 |
| 10 | `FindFriendResponse`, `FriendCursor`, `FriendPageResponse`, `FriendshipsRepository`, `FriendshipsPersistenceAdapter` | порт + адаптер | страница друзей | Р2-4…Р2-6 |
| 11 | `GetFriendsCommand`, `FriendResult`, `FriendCursorResult`, `FriendPageResult`, `IncompleteFriendCursorException`, `FriendshipApplicationMapper`, `GetFriends` | application | use case | Р1-8, Р2-2 |
| 12 | `FriendshipWebMapper`, `FriendshipController`, `FriendshipOfferExceptionHandler` | web | friends | Р1-6 |
| 13 | `FriendshipE2ETest` | тест | новый | Р4-6 |
| 14 | `manual-test-curl.md`, `manual-test-bruno.md`, `bruno/40000/` | ручные тесты | новые | Р1-11 |

Подзадачи: **A** — п. 1–4 (контракт; сборка красная до п. 8); **B** — п. 5–9 (заявки; сборка и тесты зелёные);
**C** — п. 10–12 (друзья); **D** — п. 13–14 (тесты).

---

## Подзадача A. Контракт

## 1. `GET /friendship-offer` (Р3-2, Р3-3, Р4-1, Р4-2, Р2-1, Р2-2)
**Зачем:** один эндпоинт заявок текущего пользователя вместо трёх; схемы в файлах → понятные имена DTO.

- `schemas/friendship-offer-cursor.yaml`, `schemas/friendship-offer-page.yaml` — по образцу `post-cursor.yaml`/`post-page.yaml`
  (`required: [offers, next]`, `next` → курсор, `nullable: true`).
- `schemas/friendship-offer-direction.yaml` — `type: string`, `enum: [INCOMING, OUTGOING]`, описание: `INCOMING` — получатель текущий пользователь,
  `OUTGOING` — отправитель.
- `schemas/friendship-offer-status.yaml` — `enum: [PENDING, ACCEPTED, REJECTED]`. В `friendship-offer.yaml` не подключаем
  (иначе меняется тип поля `status` в существующем DTO).
- `paths/friendship-offers-get.yaml`:
  - `summary` «Получить страницу заявок текущего пользователя», `security: - userIdHeader: []`;
  - `description`: заявки, где текущий пользователь (X-User-Id) получатель (`INCOMING`) или отправитель (`OUTGOING`); порядок
    `createdAt DESC, id DESC`; курсор; убрать фразу «доступно без проверки пользователя»;
  - параметры: `direction` (`required: true`, `$ref` схемы), `status` (`$ref`, `default: PENDING`), `limit`/`afterCreatedAt`/`afterId` — как сейчас;
  - `200` → `$ref: '../schemas/friendship-offer-page.yaml'`, примеры `incoming` (две заявки B из `swagger-examples.sql`), `empty`;
  - `400` — добавить «`direction` нет или не из списка, `status` не из списка»; `401` → `$ref: '../responses/unauthorized.yaml'`.
- Удалить `paths/friendships-incoming-get.yaml`, `paths/friendships-outgoing-get.yaml` и их пути в `openapi.yaml`, `modules/friendships.yaml`.
- `modules/friendships.yaml` — `components.securitySchemes.userIdHeader`, как в `modules/posts.yaml`.

## 2. `friends` (Р1-1, Р2-1, Р2-4, Р2-9)
- `schemas/friend.yaml` (`id` — id дружбы, `userId` — другой пользователь пары), `schemas/friend-cursor.yaml` («позиция последней дружбы»),
  `schemas/friend-page.yaml` (`required: [friends, next]`).
- `paths/friends-get.yaml`: `userId` в query остаётся; добавить `limit`/`afterCreatedAt`/`afterId` как в п. 1; `200` → `friend-page.yaml`;
  `description` — порядок `createdAt DESC, id DESC`, только активные дружбы; пример `found` — только C (`…4e02`), `next: null`; `400` — как в п. 1.

## 3. `pom.xml` (Р2-1)
- `inline-names`: убрать `getFriendshipOffers_200_response*`, `getFriends_200_response_inner`.
- `model-names`: `friendship-offer-page=FriendshipOfferPageWebResponse`, `friendship-offer-cursor=FriendshipOfferCursorWebResponse`,
  `friendship-offer-direction=FriendshipOfferDirectionWebRequest`, `friendship-offer-status=FriendshipOfferStatusWebRequest`,
  `friend=FriendWebResponse`, `friend-page=FriendPageWebResponse`, `friend-cursor=FriendCursorWebResponse`.
- `responses` — имена файлов `friendship-offer-page,friendship-offer-cursor,friend,friend-page,friend-cursor`;
  `requests` — `+ friendship-offer-direction,friendship-offer-status`; `imports` — новые классы.
- Проверить сгенерированные `FriendshipOfferApi`/`FriendshipApi`: `direction`/`status` — enum (неизвестное → `MethodArgumentTypeMismatchException` → `400`).
  Если `String` — вопрос пользователю, не обходить молча.
- Сборка после пункта красная (сигнатура `getFriendshipOffers`, удалённые incoming/outgoing) — чинится в п. 8.

## 4. Описания `docs/api/*.md` (Р1-4, Р1-5, Р4-5)
- `friendship-offers-list.md` (новый) — по образцу `friendships-incoming-list.md`: `X-User-Id`, `direction`, `status`, курсор, `{offers, next}`,
  порядок, `400`/`401`; неизвестный пользователь → пустая страница.
- Удалить `friendships-incoming-list.md`, `friendships-outgoing-list.md`.
- `friends-list.md`: курсор, `{friends, next}`, порядок; убрать `404`.
- `followers-list.md`, `subscribes-list.md`: только убрать `404`, «неизвестный пользователь → пустой список».

---

## Подзадача B. Заявки

## 5. Индексы (Р2-5, Р4-4, Р2-10)
- `db/changes/011-friendships-page-indexes.sql` — формат как `010-…`, changeset `student:011`:
  - `DROP INDEX idx_friendship_offer_incoming`, `…_outgoing`, `…_created`;
    `CREATE INDEX idx_friendship_offer_incoming ON friendship_offer (to_user_id, status, created_at DESC, id DESC)`, то же `outgoing` с `from_user_id`;
  - `DROP INDEX idx_friendships_first_user`, `…_second_user`;
    `CREATE INDEX idx_friendships_first_user_page ON friendships (first_user_id, created_at DESC, id DESC)`, то же для `second_user_id`;
  - `--rollback` — обратно (как в `002`, `004`, `010`).
- `db.changelog-master.yaml` — `include` перед блоком `dev/swagger-examples.sql`.

## 6. Порт и адаптер заявок (Р3-1, Р4-3, Р1-5)
- `port/model/FriendshipOfferDirection` — enum `INCOMING`, `OUTGOING` с Javadoc.
- `FriendshipOfferRepository`: `findPage(UUID userId, FriendshipOfferDirection direction, FriendshipOfferStatus status, int limit)`,
  `findPageAfter(UUID userId, FriendshipOfferDirection direction, FriendshipOfferStatus status, FriendshipOfferCursor after, int limit)`;
  Javadoc — «заявки, где `userId` получатель/отправитель, в статусе `status`».
- `FriendshipOfferPersistenceAdapter`: вместо `FIND_FIRST_PAGE`/`FIND_PAGE_AFTER` — `FIND_INCOMING_FIRST_PAGE`, `FIND_INCOMING_PAGE_AFTER`,
  `FIND_OUTGOING_FIRST_PAGE`, `FIND_OUTGOING_PAGE_AFTER` (`WHERE to_user_id = ? AND status = ?` / `from_user_id`, `[AND (created_at, id) < (?, ?)]`);
  SQL выбирается `switch` по `direction`; приватный `findPage(limit, sql, args)` и `checkLimit` переиспользуются.

## 7. Use case (Р4-2, Р4-3)
- `GetFriendshipOffersCommand(UUID userId, FriendshipOfferDirection direction, FriendshipOfferStatus status, Instant afterCreatedAt, UUID afterId, int limit)`.
- `GetFriendshipOffers`: передать `userId`, `direction`, `status` в порт; Javadoc класса — «заявки текущего пользователя», убрать «reader is not checked».

## 8. Web (Р3-2, Р3-3)
- `FriendshipOfferWebMapper.toGetPageCommand(UUID userId, FriendshipOfferDirectionWebRequest direction, FriendshipOfferStatusWebRequest status,
  Integer limit, Instant afterCreatedAt, UUID afterId)` + маппинг enum по имени.
- `FriendshipOfferController`: зависимость `CurrentUserProvider` (как `FeedController`); `getFriendshipOffers(direction, status, limit, afterCreatedAt, afterId)`
  → `currentUser.currentUserId()` → use case; Javadoc + «нет/не UUID `X-User-Id` → `401` (`GlobalExceptionHandler`)».
  Удалить `getIncomingFriendshipOffers`/`getOutgoingFriendshipOffers`.
- `./mvnw -q compile` — зелёная.

## 9. Тесты заявок (Р4-6)
- `FriendshipOfferPersistenceAdapterTest`: страницы по пользователю и направлению (своя пара случайных пользователей — хак `newerThanAll` не нужен),
  фильтр `status`, чужие заявки не попадают, конец списка, `limit` вне диапазона.
- `GetFriendshipOffersTest`: фильтр передаётся в порт; неполный курсор.
- `FriendshipOfferGetControllerTest`: заголовок `X-User-Id`, `direction`; + без `direction` → `400`, `direction=FOO` → `400`, без `X-User-Id` → `401`.
- `FriendshipOfferE2ETest.GetPage`: `INCOMING`/`OUTGOING` своего пользователя, вторая страница по `next`, `status=ACCEPTED`, пусто;
  удалить `IncomingOutgoing`.
- `FriendshipOfferWebMapperTest`: `toGetPageCommand` с новыми параметрами.
- `./mvnw test` (нужен Docker).

---

## Подзадача C. Друзья

## 10. Порт и адаптер друзей (Р2-4…Р2-6)
- `port/model`: `FindFriendResponse(UUID id, UUID userId)`, `FriendCursor(Instant createdAt, UUID id)`, `FriendPageResponse(List<FindFriendResponse> friends, Optional<FriendCursor> next)`.
- `FriendshipsRepository`: `MAX_LIMIT`, `findFriendsPage(UUID userId, int limit)`, `findFriendsPageAfter(UUID userId, FriendCursor after, int limit)`.
- `FriendshipsPersistenceAdapter`:
  ```sql
  SELECT id, second_user_id AS friend_id, created_at FROM friendships
   WHERE first_user_id = ? AND status = 'ACTIVE' [AND (created_at, id) < (?, ?)]
  UNION ALL
  SELECT id, first_user_id, created_at FROM friendships
   WHERE second_user_id = ? AND status = 'ACTIVE' [AND (created_at, id) < (?, ?)]
  ORDER BY created_at DESC, id DESC
  LIMIT ?
  ```
  `limit + 1` → `next` по последней строке страницы (как у заявок).

## 11. Use case друзей (Р3-3, Р1-8, Р2-2)
- `model`: `GetFriendsCommand(UUID userId, Instant afterCreatedAt, UUID afterId, int limit)`, `FriendResult(UUID id, UUID userId)`,
  `FriendCursorResult(Instant createdAt, UUID id)`, `FriendPageResult(List<FriendResult> friends, FriendCursorResult next)`.
- `exception/IncompleteFriendCursorException` — по образцу `IncompleteFriendshipOfferCursorException`.
- `FriendshipApplicationMapper`: `FriendPageResult toResult(FriendPageResponse)` + `unwrapNext` как в `FriendshipOfferApplicationMapper`.
- `GetFriends` — по образцу `GetFriendshipOffers`.

## 12. Web друзей (Р1-6)
- `FriendshipWebMapper` (MapStruct): `toCommand(UUID userId, Integer limit, Instant afterCreatedAt, UUID afterId)`, `FriendPageWebResponse toResponse(FriendPageResult)`.
- `FriendshipController implements FriendshipApi` — use case → `200`.
- `FriendshipOfferExceptionHandler`: `assignableTypes = {FriendshipOfferController.class, FriendshipController.class}`,
  в список `400` — `IncompleteFriendCursorException`; Javadoc класса — «оба контроллера модуля».

---

## Подзадача D. Тесты

## 13. `FriendshipE2ETest` (Р4-6)
`@E2ETest`, случайные пользователи; дружба создаётся через create + accept.
- друг после accept (`id` дружбы, `userId` — другой участник) — с обеих сторон пары; пусто → `{friends: [], next: null}`; `userId=abc` → `400 VALIDATION_ERROR`.

После пункта: `./mvnw test`. Напоминание: `ApplicationModulesTest` — по команде пользователя.

## 14. Ручные тесты (Р1-11)
- `manual-test-curl.md` — sh-скрипт: заявки A→B, C→B, принять одну; `GET /friendship-offer` `INCOMING`/`OUTGOING`/`status=ACCEPTED`,
  вторая страница, без `direction` → 400, без `X-User-Id` → 401; `friends`; итог OK/FAIL.
- `manual-test-bruno.md` — по каждому запросу: curl (`{{baseUrl}}`, `{{collection_dev_token}}`, `X-User-Id`), метод, JSON, ожидаемый ответ.
- `bruno/40000/` — коллекция по образцу `bruno/41225/`.
