# 40000 — Реализовать операции контракта, которых нет в коде

Контракт `docs/api/openapi.yaml` описывает 18 операций, в коде работают 15. Остальные три видны в Swagger UI,
но отвечают 501 или 404. Задача — реализовать их по контракту и закрыть расхождения между контрактом,
описаниями `docs/api/*.md` и кодом.

Найдено при проверке Swagger UI в задаче 41226 (19.11.2026): сверка `operationId` из `docs/api/paths`
с методами контроллеров и заглушками `NOT_IMPLEMENTED`.

## 1. Не реализовано

| № | Операция | Контракт | Описание | Сейчас |
|---|---|---|---|---|
| 1 | `GET /api/v1/friendship-offer/incoming?userId=` — `getIncomingFriendshipOffers` | `paths/friendships-incoming-get.yaml` | `friendships-incoming-list.md` | заглушка, **501** без тела (`FriendshipOfferController#getIncomingFriendshipOffers`) |
| 2 | `GET /api/v1/friendship-offer/outgoing?userId=` — `getOutgoingFriendshipOffers` | `paths/friendships-outgoing-get.yaml` | `friendships-outgoing-list.md` | заглушка, **501** без тела (`FriendshipOfferController#getOutgoingFriendshipOffers`) |
| 3 | `GET /api/v1/friendships?userId=` — `getFriends` | `paths/friends-get.yaml` | `friends-list.md` | интерфейс `FriendshipApi` генерируется, но контроллера нет — **404** `No static resource` |

### 1.1. Входящие заявки (`incoming`)
- Заявки, где `toUserId = userId`, только в статусе `PENDING`; ответ — массив `friendship-offer`
  (`id, fromUserId, toUserId, status, createdAt, updatedAt`).
- Нет заявок → `200 []`; некорректный или отсутствующий `userId` → `400 VALIDATION_ERROR`.
- Для запроса уже есть индекс `idx_friendship_offer_incoming (to_user_id, status, created_at DESC)`.

### 1.2. Исходящие заявки (`outgoing`)
- Как п. 1.1, но `fromUserId = userId`; индекс `idx_friendship_offer_outgoing (from_user_id, status, created_at DESC)`.

### 1.3. Список друзей (`friends`)
- Строки `friendships` пользователя (`first_user_id` или `second_user_id` = `userId`, пара хранится в
  каноническом порядке), только `status = ACTIVE`.
- Элемент ответа: `id` — id дружбы, `userId` — **другой** пользователь пары.
- Нет друзей → `200 []`; некорректный `userId` → `400 VALIDATION_ERROR`.
- Индексы `idx_friendships_first_user`, `idx_friendships_second_user` есть.
- Нужен новый контроллер `FriendshipController implements FriendshipApi` в `friendships/internal/infrastructure/web`;
  чтения в порту `FriendshipsRepository` пока нет.

### Что понадобится (оценка)
Для каждой операции: use case в `application/usecase`, модель команды и результата, метод порта
(`FriendshipOfferRepository` / `FriendshipsRepository`), SQL в persistence-адаптере, маппинг web ↔ application,
метод контроллера. Тесты — минимальные: архитектура и happy-path (E2E на каждую операцию). Всего ~10–15 файлов.

## 2. Вопросы (решить в fix.md)

1. **Порядок элементов.** Контракт порядок не задаёт. Предложение: заявки — `createdAt DESC, id DESC` (как индексы
   и `GET /friendship-offer`), друзья — по дате дружбы `createdAt DESC`.
2. **Пагинация.** Все три списка в контракте возвращают массив целиком, без `limit` и курсора, а `GET /friendship-offer`
   и лента уже читают по курсору. Оставить массив, как в контракте, или менять контракт на страницу с курсором?
3. **404 «Пользователь не найден».** `friends-list.md`, `friendships-incoming-list.md`, `friendships-outgoing-list.md`,
   а также уже реализованные `followers-list.md` и `subscribes-list.md` обещают `404`. В yaml-контракте такого ответа нет,
   и таблицы пользователей в проекте нет — проверить существование пользователя нечем. Предложение: убрать `404`
   из `*.md` (неизвестный пользователь → `200 []`), контракт не менять.
4. **Статусы в `incoming`/`outgoing`.** `*.md` говорят «по умолчанию `PENDING`» (то есть подразумевается фильтр по статусу).
   Нужен ли параметр `status`, или всегда только `PENDING`? Предложение: только `PENDING`, без параметра.
5. **Текущий пользователь.** Операции принимают `userId` в query, без `X-User-Id`: любой может прочитать чужие заявки
   и друзей. Оставить как в контракте до Keycloak (как `followers`/`subscribes`) или перевести на `X-User-Id`?

## 3. Проверка

- Swagger UI (`http://localhost:8080/swagger-ui.html`) при запуске с `LIQUIBASE_CONTEXTS=dev`: в базе уже есть данные
  примеров контракта (41226, Р-13, `db/dev/swagger-examples.sql`). Ожидания с подставленными примерами:
  - incoming для B `5e2a7d40…` → заявки `9c4e1b77…` (A → B) и `2f6b9d13…` (C → B);
  - outgoing для A `0b6f3c9e…` → `9c4e1b77…` (A → B) и `e8a4c2d6…` (A → D);
  - friends для A → `{id: c1d2e3f4-…4e02, userId: 7d1f0a52…}` (C). Пример контракта с другом B (`…4e01`)
    противоречит PENDING-заявке A → B, в данных его нет.
- Ручные тесты по процессу: `manual-test-curl.md` и коллекция Bruno.
- `ApplicationModulesTest` — по команде пользователя.
