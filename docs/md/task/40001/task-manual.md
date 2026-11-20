# 40001 — create/accept/reject: действующий пользователь из `X-User-Id`. План

Режим Ментора: ДА. Эталон — `decisions.md`; при расхождении плана с решением прав `decisions.md`.
После проверки плана Claude выполняет все пункты подряд, вопросы — в конце (Р1-10).

Суть:
- `POST /friendship-offer/create` — отправитель из `X-User-Id`, в теле только `toUserId` (Р0-1, Р1-1).
- `POST /friendship-offer/accept`, `/reject` — действующий из `X-User-Id`, в теле только `offerId` (Р0-1, Р1-1).
- Ответы не меняются (Р1-3); лишнее старое поле в теле игнорируется (Р1-2).
- Нет `X-User-Id` или не UUID → `401` (`GlobalExceptionHandler`); невалидное тело без заголовка → `400` (Р1-5).

| Итого | Новые | Изменить | Удалить |
|---|---|---|---|
| файлов | ~10 (2 ручных теста + коллекция `bruno/40001`) | 21 | 0 |

Не меняются: application, domain, persistence, команды `*FriendshipOfferCommand` (Р1-4), `pom.xml`
(имена сгенерированных `*WebRequest` остаются), `GET /friendship-offer[/{offerId}]`, `OpenApiE2ETest`.

## Где будут файлы

```
docs/api/
├── paths/
│   ├── friendship-offer-create.yaml                   (изменить) security, тело {toUserId}, 401, описание 400
│   ├── friendship-offer-accept.yaml                   (изменить) security, тело {offerId}, 401, описания 400/404
│   └── friendship-offer-reject.yaml                   (изменить) как accept
├── friendship-offer-create.md                         (изменить) X-User-Id, тело, ошибки 401 и 400-без-заголовка
├── friendship-offer-accept.md                         (изменить) то же
└── friendship-offer-reject.md                         (изменить) то же

src/main/java/ru/job4j/socialmediaapi/friendships/internal/infrastructure/web/
├── FriendshipOfferController.java                     (изменить) currentUser.currentUserId() в create/accept/reject
└── mapper/FriendshipOfferWebMapper.java               (изменить) to*Command(UUID, request)

src/test/java/ru/job4j/socialmediaapi/
├── FriendshipFeedTest.java                            (изменить) заголовок вместо полей тела
└── friendships/internal/infrastructure/web/
    ├── FriendshipOfferControllerTest.java             (изменить) заголовок; кейсы валидации без fromUserId/actorId
    ├── FriendshipOfferE2ETest.java                    (изменить) заголовок; + 401 без заголовка; 404 с чужим X-User-Id
    ├── FriendshipOfferGetControllerTest.java          (изменить) создание/принятие через заголовок
    ├── FriendshipE2ETest.java                         (изменить) создание/принятие через заголовок
    └── mapper/FriendshipOfferWebMapperTest.java       (изменить) to*Command(UUID, request)

docs/md/api-manual-test/
├── friendship-offer.md, feed.md, subscription.md      (изменить) заголовок вместо полей тела

bruno/
├── 40000/01–04 *.bru                                  (изменить) заголовок вместо полей тела
└── 40001/                                             (новый) коллекция ручных тестов

docs/md/task/40001/
├── manual-test-curl.md                                (новый)
└── manual-test-bruno.md                               (новый)
```

## Файлы

### Подзадача A — контракт, код, тесты (14 файлов)

| № | Файл | Слой | Зачем | Что сделать | Решение |
|---|---|---|---|---|---|
| A1 | `paths/friendship-offer-create.yaml` | контракт | отправитель из заголовка | `security: - userIdHeader: []`; из `required`/`properties` убрать `fromUserId`; `'401': $ref: '../responses/unauthorized.yaml'`; `400` — «нет `toUserId`, не UUID, `toUserId` совпадает с X-User-Id»; описание операции про X-User-Id | Р1-1, Р1-6 |
| A2 | `paths/friendship-offer-accept.yaml` | контракт | действующий из заголовка | то же для `actorId`; `404` — «X-User-Id не получатель» | Р1-1 |
| A3 | `paths/friendship-offer-reject.yaml` | контракт | как A2 | как A2 | Р1-1 |
| A4 | `FriendshipOfferWebMapper` | web | команда из заголовка и тела | `toCommand(UUID fromUserId, CreateFriendshipOfferWebRequest)`, `toAcceptCommand(UUID actorId, Accept…WebRequest)`, `toRejectCommand(UUID actorId, Reject…WebRequest)`; Javadoc | Р1-4 |
| A5 | `FriendshipOfferController` | web | правило `CurrentUserProvider` | в create/accept/reject `UUID userId = currentUser.currentUserId()` → маппер; Javadoc методов и поля `currentUser` (401) | Р0-1, Р1-4 |
| A6 | `FriendshipOfferWebMapperTest` | тест | | вызовы с `UUID` первым аргументом | Р1-7 |
| A7 | `FriendshipOfferControllerTest` | тест | | `header("X-User-Id", …)`; из таблиц валидации убрать строки `fromUserId`/`actorId`; «самому себе» — `X-User-Id` = `toUserId` | Р1-7, Р1-6 |
| A8 | `FriendshipOfferE2ETest` | тест | | хелперы `create/accept/reject` с заголовком; убрать кейсы `fromUserId`/`actorId` из валидации; + create без `X-User-Id` → `401`; кейс «чужой actorId» → чужой `X-User-Id` → `404` | Р1-7 |
| A9 | `FriendshipOfferGetControllerTest` | тест | | `createOffer`/accept — через заголовок | Р1-7 |
| A10 | `FriendshipE2ETest` | тест | | создание/принятие через заголовок | Р1-7 |
| A11 | `FriendshipFeedTest` | тест | | создание/принятие/отклонение через заголовок | Р1-7 |
| A12–A14 | `docs/api/friendship-offer-{create,accept,reject}.md` | док | описание API | заголовок `X-User-Id`, тело без поля, ответ `401`, строка «невалидное тело без заголовка → `400`» | Р1-1, Р1-5, Р1-6 |

Проверка: `mvnw test` по классам A6–A11 (нужен Docker).

### Подзадача B — ручные сценарии прошлых задач (7 файлов)

| № | Файл | Что сделать | Решение |
|---|---|---|---|
| B1–B3 | `docs/md/api-manual-test/{friendship-offer,feed,subscription}.md` | curl create/accept/reject: `-H "X-User-Id: …"`, тело без `fromUserId`/`actorId` | Р1-8 |
| B4–B7 | `bruno/40000/01–04 *.bru` | заголовок `X-User-Id`, тело без поля | Р1-8 |

### Подзадача C — ручные тесты 40001 (последний пункт)

| № | Файл | Что сделать | Решение |
|---|---|---|---|
| C1 | `manual-test-curl.md` | скрипт sh: create с заголовком → `201`; create без заголовка → `401`; create на себя → `400`; accept чужим → `404`; accept получателем → `200`; reject → `200`; старое поле в теле игнорируется → `201`; итог OK/FAIL | Р1-2, Р1-5, Р1-6 |
| C2 | `manual-test-bruno.md` + `bruno/40001/` | те же запросы: curl (`{{collection_dev_token}}`, `{{baseUrl}}`), метод, JSON, ожидаемый ответ; коллекция как `bruno/40000` | Р1-8 |

По окончании — напомнить про `ApplicationModulesTest` (не запускать без команды).
