# Проверка API заявок в друзья `/api/v1/friendship-offer`

Справка по эндпоинтам `FriendshipOfferController` и сценарий их ручной проверки на запущенном приложении.
Источник правды — контракт `docs/api/openapi.yaml` (пути `docs/api/paths/friendship-offer*.yaml`,
`friendships-{incoming,outgoing}-get.yaml`); коды ошибок — `FriendshipOfferExceptionHandler`.

У каждого эндпоинта одинаковые разделы:

- **Запрос** — метод, путь, параметры или тело;
- **Ответ** — успешный статус и пример тела;
- **Ошибки** — статусы и коды ошибок;
- **Проверка** — команды `curl` и ожидаемый результат каждой.

Команды выполняются сверху вниз в одном терминале: переменные (`OFFER`, `OFFER2`, `NEXT_AT`, …) создаются
в предыдущих разделах. Они рассчитаны на `zsh`/`bash` (macOS, Linux, Git Bash), нужны `curl`, `jq` и `uuidgen`.

Тело любой ошибки — `{"code": "...", "message": "..."}`. Для всех `400` код один:

```json
{"code":"INVALID_FRIENDSHIP_OFFER_REQUEST","message":"Некорректное тело запроса, отсутствуют обязательные поля или пользователь предлагает дружбу самому себе"}
```

| Эндпоинт | Назначение |
| --- | --- |
| [`POST /create`](#1-post-apiv1friendship-offercreate) | Создать заявку |
| [`POST /accept`](#2-post-apiv1friendship-offeraccept) | Принять заявку |
| [`POST /reject`](#3-post-apiv1friendship-offerreject) | Отклонить заявку |
| [`GET /{offerId}`](#4-get-apiv1friendship-offerofferid) | Заявка по id |
| [`GET` без суффикса](#5-get-apiv1friendship-offer) | Страница всех заявок |
| [`GET /incoming`, `/outgoing`](#6-get-apiv1friendship-offerincoming-outgoing) | Не реализованы |

## Подготовка

БД поднимается на `localhost:5435`, приложение — на `http://localhost:8080` (в отдельном терминале).

```shell
docker compose up -d postgres
./mvnw spring-boot:run
```

Переменные: пользователи каждый раз новые, поэтому сценарий можно повторять с начала, не очищая БД.

```shell
API=http://localhost:8080/api/v1/friendship-offer
ALICE=$(uuidgen | tr 'A-Z' 'a-z')
BOB=$(uuidgen | tr 'A-Z' 'a-z')
CAROL=$(uuidgen | tr 'A-Z' 'a-z')
DAVE=$(uuidgen | tr 'A-Z' 'a-z')
echo "alice=$ALICE bob=$BOB carol=$CAROL dave=$DAVE"
```

`curl -i` печатает статус и заголовки ответа, `-w '\n'` добавляет перевод строки после тела.

---

## 1. `POST /api/v1/friendship-offer/create`

Создаёт заявку `PENDING`; отправитель в той же транзакции подписывается на получателя. У пары пользователей
не больше одной заявки в любом направлении. Полное описание — `docs/api/friendship-offer-create.md`.

### Запрос

```json
{"fromUserId": "<uuid>", "toUserId": "<uuid>"}
```

| Поле | Тип | Обязательное | Описание |
| --- | --- | --- | --- |
| `fromUserId` | `string (uuid)` | Да | Отправитель |
| `toUserId` | `string (uuid)` | Да | Получатель, не равен отправителю |

### Ответ

`201 Created`:

```json
{"id":"…","fromUserId":"…","toUserId":"…","status":"PENDING","createdAt":"2026-11-04T10:15:00.123456Z"}
```

### Ошибки

| Статус | `code` | Ситуация |
| --- | --- | --- |
| `200 OK` | `FRIENDSHIP_OFFER_ALREADY_PENDING` | Заявка пары (в любом направлении) уже ждёт решения; ничего не меняется |
| `400` | `INVALID_FRIENDSHIP_OFFER_REQUEST` | Тело не читается, нет поля, id не UUID, заявка самому себе |
| `409` | `FRIENDSHIP_OFFER_ALREADY_ACCEPTED` | Заявка пары уже принята |
| `409` | `FRIENDSHIP_OFFER_ALREADY_REJECTED` | Заявка пары уже отклонена |

`200` с телом ошибки — решение контракта: повтор не ошибка клиента, а `204` не может нести тело.

### Проверка

**1.1. Заявка Alice → Bob** → `201`. Заголовки печатаются в stderr, id сохраняется в `OFFER`.

```shell
OFFER=$(curl -s -D /dev/stderr -X POST "$API/create" -H 'Content-Type: application/json' \
  -d "{\"fromUserId\":\"$ALICE\",\"toUserId\":\"$BOB\"}" | tee /dev/stderr | jq -r .id); echo; echo "offer=$OFFER"
```

**1.2. Заявка Carol → Dave** → `201`, id в `OFFER2` (её отклонят в разделе 3).

```shell
OFFER2=$(curl -s -D /dev/stderr -X POST "$API/create" -H 'Content-Type: application/json' \
  -d "{\"fromUserId\":\"$CAROL\",\"toUserId\":\"$DAVE\"}" | jq -r .id); echo "offer2=$OFFER2"
```

**1.3. Повтор Alice → Bob** → `200 FRIENDSHIP_OFFER_ALREADY_PENDING`.

```shell
curl -i -X POST "$API/create" -H 'Content-Type: application/json' \
  -d "{\"fromUserId\":\"$ALICE\",\"toUserId\":\"$BOB\"}" -w '\n'
```

**1.4. Встречная Bob → Alice** → тот же `200 FRIENDSHIP_OFFER_ALREADY_PENDING`.

```shell
curl -i -X POST "$API/create" -H 'Content-Type: application/json' \
  -d "{\"fromUserId\":\"$BOB\",\"toUserId\":\"$ALICE\"}" -w '\n'
```

**1.5. Некорректные запросы** → все `400 INVALID_FRIENDSHIP_OFFER_REQUEST`. По порядку: заявка самому себе,
нет `toUserId`, id не UUID, тело не JSON.

```shell
curl -i -X POST "$API/create" -H 'Content-Type: application/json' \
  -d "{\"fromUserId\":\"$ALICE\",\"toUserId\":\"$ALICE\"}" -w '\n'
curl -i -X POST "$API/create" -H 'Content-Type: application/json' \
  -d "{\"fromUserId\":\"$ALICE\"}" -w '\n'
curl -i -X POST "$API/create" -H 'Content-Type: application/json' \
  -d '{"fromUserId":"1","toUserId":"2"}' -w '\n'
curl -i -X POST "$API/create" -H 'Content-Type: application/json' -d '{oops' -w '\n'
```

Повторы для принятой и отклонённой пары (`409`) проверяются в разделах 2 и 3, после решения.

---

## 2. `POST /api/v1/friendship-offer/accept`

Принимает заявку `PENDING`. Принять может только получатель. В одной транзакции создаются дружба и подписка
получателя на отправителя. Полное описание — `docs/api/friendship-offer-accept.md`.

### Запрос

```json
{"offerId": "<uuid>", "actorId": "<uuid>"}
```

| Поле | Тип | Обязательное | Описание |
| --- | --- | --- | --- |
| `offerId` | `string (uuid)` | Да | Заявка |
| `actorId` | `string (uuid)` | Да | Кто принимает — должен быть получателем (до Keycloak передаётся в теле) |

### Ответ

`200 OK`:

```json
{"id":"…","fromUserId":"…","toUserId":"…","status":"ACCEPTED","createdAt":"…","acceptedAt":"…"}
```

### Ошибки

| Статус | `code` | Ситуация |
| --- | --- | --- |
| `400` | `INVALID_FRIENDSHIP_OFFER_REQUEST` | Тело не читается, нет поля, id не UUID |
| `404` | `FRIENDSHIP_OFFER_NOT_FOUND` | Заявки нет или `actorId` не её получатель (чужая заявка не раскрывается) |
| `409` | `FRIENDSHIP_OFFER_ALREADY_ACCEPTED` | Заявка уже принята, в том числе повтор |
| `409` | `FRIENDSHIP_OFFER_ALREADY_REJECTED` | Заявка уже отклонена |

### Проверка

**2.1. Принять от имени отправителя (Alice)** → `404 FRIENDSHIP_OFFER_NOT_FOUND`.

```shell
curl -i -X POST "$API/accept" -H 'Content-Type: application/json' \
  -d "{\"offerId\":\"$OFFER\",\"actorId\":\"$ALICE\"}" -w '\n'
```

**2.2. Принять от имени получателя (Bob)** → `200`, `status: ACCEPTED`, есть `acceptedAt`.

```shell
curl -i -X POST "$API/accept" -H 'Content-Type: application/json' \
  -d "{\"offerId\":\"$OFFER\",\"actorId\":\"$BOB\"}" -w '\n'
```

**2.3. Принять ещё раз** → `409 FRIENDSHIP_OFFER_ALREADY_ACCEPTED`.

```shell
curl -i -X POST "$API/accept" -H 'Content-Type: application/json' \
  -d "{\"offerId\":\"$OFFER\",\"actorId\":\"$BOB\"}" -w '\n'
```

**2.4. Создать заявку для подружившейся пары** (любое направление) → `409 FRIENDSHIP_OFFER_ALREADY_ACCEPTED`.

```shell
curl -i -X POST "$API/create" -H 'Content-Type: application/json' \
  -d "{\"fromUserId\":\"$BOB\",\"toUserId\":\"$ALICE\"}" -w '\n'
```

**2.5. Несуществующая заявка** → `404 FRIENDSHIP_OFFER_NOT_FOUND`; **нет `actorId`** → `400`.

```shell
curl -i -X POST "$API/accept" -H 'Content-Type: application/json' \
  -d "{\"offerId\":\"$(uuidgen)\",\"actorId\":\"$BOB\"}" -w '\n'
curl -i -X POST "$API/accept" -H 'Content-Type: application/json' \
  -d "{\"offerId\":\"$OFFER\"}" -w '\n'
```

---

## 3. `POST /api/v1/friendship-offer/reject`

Отклоняет заявку `PENDING`. Отклонить может только получатель. Дружба не создаётся, подписка отправителя
остаётся. Полное описание — `docs/api/friendship-offer-reject.md`.

### Запрос

Тело — как у `accept`: `{"offerId": "<uuid>", "actorId": "<uuid>"}`, `actorId` — получатель заявки.

### Ответ

`200 OK`:

```json
{"id":"…","fromUserId":"…","toUserId":"…","status":"REJECTED","createdAt":"…","rejectedAt":"…"}
```

### Ошибки

| Статус | `code` | Ситуация |
| --- | --- | --- |
| `400` | `INVALID_FRIENDSHIP_OFFER_REQUEST` | Тело не читается, нет поля, id не UUID |
| `404` | `FRIENDSHIP_OFFER_NOT_FOUND` | Заявки нет или `actorId` не её получатель |
| `409` | `FRIENDSHIP_OFFER_ALREADY_ACCEPTED` | Заявка уже принята |
| `409` | `FRIENDSHIP_OFFER_ALREADY_REJECTED` | Заявка уже отклонена, в том числе повтор |

### Проверка

**3.1. Отклонить от имени получателя (Dave)** → `200`, `status: REJECTED`, есть `rejectedAt`.

```shell
curl -i -X POST "$API/reject" -H 'Content-Type: application/json' \
  -d "{\"offerId\":\"$OFFER2\",\"actorId\":\"$DAVE\"}" -w '\n'
```

**3.2. Отклонить ещё раз** → `409 FRIENDSHIP_OFFER_ALREADY_REJECTED`.

```shell
curl -i -X POST "$API/reject" -H 'Content-Type: application/json' \
  -d "{\"offerId\":\"$OFFER2\",\"actorId\":\"$DAVE\"}" -w '\n'
```

**3.3. Принять отклонённую** → `409 FRIENDSHIP_OFFER_ALREADY_REJECTED`.

```shell
curl -i -X POST "$API/accept" -H 'Content-Type: application/json' \
  -d "{\"offerId\":\"$OFFER2\",\"actorId\":\"$DAVE\"}" -w '\n'
```

**3.4. Отклонить принятую (из раздела 2)** → `409 FRIENDSHIP_OFFER_ALREADY_ACCEPTED`.

```shell
curl -i -X POST "$API/reject" -H 'Content-Type: application/json' \
  -d "{\"offerId\":\"$OFFER\",\"actorId\":\"$BOB\"}" -w '\n'
```

**3.5. Создать заявку для отклонённой пары** → `409 FRIENDSHIP_OFFER_ALREADY_REJECTED`.

```shell
curl -i -X POST "$API/create" -H 'Content-Type: application/json' \
  -d "{\"fromUserId\":\"$CAROL\",\"toUserId\":\"$DAVE\"}" -w '\n'
```

---

## 4. `GET /api/v1/friendship-offer/{offerId}`

Возвращает одну заявку в любом статусе. Ничего не меняет. Участник заявки не проверяется, пока не подключена
аутентификация (Keycloak).

### Запрос

| Параметр | Где | Тип | Обязательный | Описание |
| --- | --- | --- | --- | --- |
| `offerId` | путь | `string (uuid)` | Да | Заявка |

### Ответ

`200 OK`:

```json
{
  "id": "9c4e1b77-3f2a-4d6c-8b0e-1a5f7d2c3e44",
  "fromUserId": "0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01",
  "toUserId": "5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22",
  "status": "ACCEPTED",
  "createdAt": "2026-09-22T10:15:00.123456Z",
  "updatedAt": "2026-09-22T10:20:00.654321Z"
}
```

| Поле | Тип | Описание |
| --- | --- | --- |
| `id` | `string (uuid)` | Заявка |
| `fromUserId` | `string (uuid)` | Отправитель |
| `toUserId` | `string (uuid)` | Получатель |
| `status` | `string` | `PENDING`, `ACCEPTED` или `REJECTED` |
| `createdAt` | `string` | Время создания, ISO 8601 в UTC |
| `updatedAt` | `string` | Время последнего изменения; у решённой заявки — время принятия или отклонения |

### Ошибки

| Статус | `code` | Ситуация |
| --- | --- | --- |
| `400` | `INVALID_FRIENDSHIP_OFFER_REQUEST` | `offerId` не UUID |
| `404` | `FRIENDSHIP_OFFER_NOT_FOUND` | Заявки с таким id нет |

### Проверка

**4.1. Принятая и отклонённая заявки** → `200`; `status` `ACCEPTED` и `REJECTED`, `updatedAt` — время решения.

```shell
curl -i "$API/$OFFER" -w '\n'
curl -i "$API/$OFFER2" -w '\n'
```

**4.2. Несуществующая заявка** → `404 FRIENDSHIP_OFFER_NOT_FOUND`; **id не UUID** → `400`.

```shell
curl -i "$API/$(uuidgen)" -w '\n'
curl -i "$API/42" -w '\n'
```

---

## 5. `GET /api/v1/friendship-offer`

Возвращает страницу всех заявок в любом статусе, от новых к старым (`createdAt DESC`, при равном времени —
`id DESC`). Ничего не меняет, пользователь не проверяется (до Keycloak).

Страницы читаются по курсору, как лента (`docs/process/Feed.md`): без `OFFSET`, поэтому стоимость страницы
не зависит от её номера, а заявки, созданные во время чтения, не сдвигают страницы. Первая страница — без
курсора, каждая следующая — с курсором из `next` предыдущего ответа.

### Запрос

| Параметр | Где | Тип | Обязательный | Описание |
| --- | --- | --- | --- | --- |
| `limit` | query | `integer` | Нет | Размер страницы, 1..100, по умолчанию 20 |
| `afterCreatedAt` | query | `string (date-time)` | Нет | `next.createdAt` предыдущей страницы; вместе с `afterId` |
| `afterId` | query | `string (uuid)` | Нет | `next.id` предыдущей страницы; вместе с `afterCreatedAt` |

`afterCreatedAt` передаётся как есть: в нём микросекунды, без них часть заявок пропадёт или повторится.

### Ответ

`200 OK`:

```json
{
  "offers": [
    {"id": "…", "fromUserId": "…", "toUserId": "…", "status": "PENDING", "createdAt": "…", "updatedAt": "…"}
  ],
  "next": {"createdAt": "2026-09-22T10:15:00.123456Z", "id": "…"}
}
```

| Поле | Описание |
| --- | --- |
| `offers` | Заявки страницы, поля — как в разделе 4. Может быть пустым |
| `next` | Курсор следующей страницы — позиция последней заявки страницы; `null`, если заявок больше нет |

### Ошибки

| Статус | `code` | Ситуация |
| --- | --- | --- |
| `400` | `INVALID_FRIENDSHIP_OFFER_REQUEST` | `limit` вне 1..100 или не число, `afterCreatedAt` не дата-время, `afterId` не UUID, задана только одна часть курсора |

### Проверка

**5.1. Первая страница** → `200`, сверху заявки Carol → Dave (`OFFER2`) и Alice → Bob (`OFFER`) из раздела 1.
Курсор сохраняется в `NEXT_AT` и `NEXT_ID`. Если кто-то ещё создавал заявки, сверху будут они — порядок всё
равно от новых к старым.

```shell
PAGE=$(curl -s "$API?limit=2"); echo "$PAGE" | jq
NEXT_AT=$(echo "$PAGE" | jq -r .next.createdAt); NEXT_ID=$(echo "$PAGE" | jq -r .next.id); echo "next=$NEXT_AT $NEXT_ID"
```

**5.2. Следующая страница** → заявки старше `OFFER`, без повторов; на последней странице `next: null`.

```shell
curl -s "$API?limit=2&afterCreatedAt=$NEXT_AT&afterId=$NEXT_ID" | jq
```

**5.3. Без параметров** → не больше 20 заявок.

```shell
curl -s "$API" | jq '.offers | length'
```

**5.4. Некорректные параметры** → все `400`. По порядку: `limit` 0, 101, не число, только одна часть курсора,
`afterCreatedAt` не дата-время.

```shell
curl -i "$API?limit=0" -w '\n'
curl -i "$API?limit=101" -w '\n'
curl -i "$API?limit=abc" -w '\n'
curl -i "$API?afterCreatedAt=$NEXT_AT" -w '\n'
curl -i "$API?afterCreatedAt=yesterday&afterId=$NEXT_ID" -w '\n'
```

---

## 6. `GET /api/v1/friendship-offer/{incoming, outgoing}`

Входящие (пользователь — получатель) и исходящие (пользователь — отправитель) заявки пользователя.
Есть в контракте и в сгенерированном `FriendshipOfferApi`, но ещё не реализованы: метод отвечает `501`.

### Запрос

| Параметр | Где | Тип | Обязательный | Описание |
| --- | --- | --- | --- | --- |
| `userId` | query | `string (uuid)` | Да | Пользователь |

### Ответ

Сейчас — `501 Not Implemented` без тела.

По контракту — `200 OK` и список заявок; элемент списка — заявка из раздела 4 (схема
`docs/api/schemas/friendship-offer.yaml`, с `updatedAt`). Нет заявок — пустой список `[]`.

```json
[
  {
    "id": "9c4e1b77-3f2a-4d6c-8b0e-1a5f7d2c3e44",
    "fromUserId": "0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01",
    "toUserId": "5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22",
    "status": "PENDING",
    "createdAt": "2026-09-22T10:15:00Z",
    "updatedAt": "2026-09-22T10:15:00Z"
  }
]
```

### Ошибки

`userId` разбирается до вызова метода, поэтому эти ошибки есть уже сейчас:

| Статус | `code` | Ситуация |
| --- | --- | --- |
| `400` | `INVALID_FRIENDSHIP_OFFER_REQUEST` | `userId` не передан или не UUID |

`404 USER_NOT_FOUND` из контракта не возвращается: таблицы пользователей пока нет.

### Проверка

**6.1. Оба эндпоинта** → `501`.

```shell
curl -i "$API/incoming?userId=$BOB" -w '\n'
curl -i "$API/outgoing?userId=$ALICE" -w '\n'
```

**6.2. `userId` не UUID и без `userId`** → оба `400 INVALID_FRIENDSHIP_OFFER_REQUEST`.

```shell
curl -i "$API/incoming?userId=1" -w '\n'
curl -i "$API/incoming" -w '\n'
```

---

## 7. Проверка данных в БД

После разделов 1–3. Значения `<alice>` и др. — из `echo` в разделе «Подготовка».

```shell
docker compose exec postgres psql -U postgres -d social_media
```

```sql
-- заявки: Alice → Bob ACCEPTED, Carol → Dave REJECTED
SELECT id, from_user_id, to_user_id, status, created_at, updated_at
FROM friendship_offer
WHERE from_user_id IN ('<alice>', '<carol>');

-- дружба только у Alice и Bob, пара хранится в каноническом порядке (first_user_id < second_user_id)
SELECT * FROM friendships
WHERE '<alice>' IN (first_user_id, second_user_id) OR '<carol>' IN (first_user_id, second_user_id);

-- подписки: Alice → Bob и Bob → Alice (после принятия), Carol → Dave (осталась после отклонения)
SELECT follower_id, followed_id FROM subscriptions
WHERE follower_id IN ('<alice>', '<bob>', '<carol>', '<dave>');
```
