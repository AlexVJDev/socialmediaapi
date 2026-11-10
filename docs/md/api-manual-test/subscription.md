# Проверка API подписок `/api/v1/subscribes`

Справка по эндпоинтам `SubscriptionController` и сценарий их ручной проверки на запущенном приложении.
Источник правды — контракт `docs/api/openapi.yaml` (пути `docs/api/paths/subscribes-get.yaml`,
`followers-get.yaml`); коды ошибок — `SubscriptionExceptionHandler` и `ErrorCode` модуля `subscriptions`.

У каждого эндпоинта одинаковые разделы:

- **Запрос** — метод, путь, параметры;
- **Ответ** — успешный статус и пример тела;
- **Ошибки** — статусы и коды ошибок;
- **Проверка** — команды `curl` и ожидаемый результат каждой.

Команды выполняются сверху вниз в одном терминале: переменные создаются в предыдущих разделах.
Они рассчитаны на `zsh`/`bash` (macOS, Linux, Git Bash), нужны `curl`, `jq` и `uuidgen`.

Тело любой ошибки — `{"code": "...", "message": "..."}` (схема `docs/api/schemas/error.yaml`):

| `code` | Статус | `message` |
| --- | --- | --- |
| `INVALID_USER_ID` | `400` | Некорректный или отсутствующий параметр userId |

Ограничения запроса — только из контракта: у обоих эндпоинтов один query-параметр `userId`
(`required: true`, `format: uuid`), других параметров (страниц, `limit`) в контракте нет. В сгенерированном
`SubscriptionApi` это `@NotNull @RequestParam(required = true) UUID userId`; тел запросов у модуля нет.

`404 USER_NOT_FOUND` есть в контракте, но не возвращается: таблицы пользователей пока нет, для неизвестного
пользователя ответ — `200` с пустым списком.

| Эндпоинт | Назначение |
| --- | --- |
| [`GET /api/v1/subscribes`](#1-get-apiv1subscribes) | На кого подписан пользователь |
| [`GET /api/v1/subscribes/followers`](#2-get-apiv1subscribesfollowers) | Кто подписан на пользователя |

## Подготовка

БД поднимается на `localhost:5435`, приложение — на `http://localhost:8080` (в отдельном терминале).

```shell
docker compose up -d postgres
./mvnw spring-boot:run
```

REST API для создания подписки нет: подписки создаются заявками в друзья (отправитель заявки подписывается на
получателя). Alice отправляет заявки Bob и Carol — у Alice две подписки, у Bob и Carol по одному подписчику.
Пользователи каждый раз новые, поэтому сценарий можно повторять, не очищая БД.

```shell
API=http://localhost:8080/api/v1/subscribes
OFFERS=http://localhost:8080/api/v1/friendship-offer
ALICE=$(uuidgen | tr 'A-Z' 'a-z')
BOB=$(uuidgen | tr 'A-Z' 'a-z')
CAROL=$(uuidgen | tr 'A-Z' 'a-z')
echo "alice=$ALICE bob=$BOB carol=$CAROL"

curl -s -o /dev/null -w '%{http_code}\n' -X POST "$OFFERS/create" -H 'Content-Type: application/json' \
  -d "{\"fromUserId\":\"$ALICE\",\"toUserId\":\"$BOB\"}"
curl -s -o /dev/null -w '%{http_code}\n' -X POST "$OFFERS/create" -H 'Content-Type: application/json' \
  -d "{\"fromUserId\":\"$ALICE\",\"toUserId\":\"$CAROL\"}"
```

Ожидаемо: `201` и `201`.

---

## 1. `GET /api/v1/subscribes`

Пользователи, на которых подписан `userId`; новые подписки первыми (`created_at DESC, id DESC`).
Полное описание — `docs/api/subscribes-list.md`.

### Запрос

| Параметр | Где | Тип | Обязательный | Описание |
| --- | --- | --- | --- | --- |
| `userId` | query | `string (uuid)` | Да | Чьи подписки читать |

### Ответ

`200 OK`, массив; пустой массив, если подписок нет. `id` — идентификатор подписки, `userId` — на кого подписан.

```json
[
  {"id": "dae828c4-dd86-45c8-8d81-beb28127a891", "userId": "<carol>"},
  {"id": "fb0cf96c-a455-43b9-8eff-7d6bb448c2e9", "userId": "<bob>"}
]
```

### Ошибки

| Статус | `code` | Когда |
| --- | --- | --- |
| `400` | `INVALID_USER_ID` | `userId` отсутствует или не UUID |

### Проверка

**1.1. Подписки Alice** → `200`, два элемента: сначала Carol (подписка новее), потом Bob.

```shell
curl -s "$API?userId=$ALICE" | jq
curl -s "$API?userId=$ALICE" | jq -r '.[].userId'   # $CAROL, затем $BOB
```

**1.2. Подписки Bob** → `200`, `[]` (Bob ни на кого не подписан).

```shell
curl -i "$API?userId=$BOB" -w '\n'
```

**1.3. Некорректный `userId`** → все `400 INVALID_USER_ID`. По порядку: не UUID, без `userId`, пустое
значение, пробел, имя в другом регистре (`userid` — это другой параметр, `userId` отсутствует).

```shell
curl -i "$API?userId=1" -w '\n'
curl -i "$API" -w '\n'
curl -i "$API?userId=" -w '\n'
curl -i "$API?userId=%20" -w '\n'
curl -i "$API?userid=$ALICE" -w '\n'
```

---

## 2. `GET /api/v1/subscribes/followers`

Пользователи, подписанные на `userId`; новые подписки первыми (`created_at DESC, id DESC`).
Полное описание — `docs/api/followers-list.md`.

### Запрос

| Параметр | Где | Тип | Обязательный | Описание |
| --- | --- | --- | --- | --- |
| `userId` | query | `string (uuid)` | Да | Чьих подписчиков читать |

### Ответ

`200 OK`, массив; пустой массив, если подписчиков нет. `id` — идентификатор подписки, `userId` — подписчик.

```json
[
  {"id": "fb0cf96c-a455-43b9-8eff-7d6bb448c2e9", "userId": "<alice>"}
]
```

### Ошибки

| Статус | `code` | Когда |
| --- | --- | --- |
| `400` | `INVALID_USER_ID` | `userId` отсутствует или не UUID |

### Проверка

**2.1. Подписчики Bob** → `200`, один элемент: Alice; `id` совпадает с `id` подписки на Bob из 1.1.

```shell
curl -s "$API/followers?userId=$BOB" | jq
```

**2.2. Подписчики Alice** → `200`, `[]`.

```shell
curl -i "$API/followers?userId=$ALICE" -w '\n'
```

**2.3. Некорректный `userId`** → все `400 INVALID_USER_ID`. По порядку: не UUID, без `userId`, пустое
значение, пробел, имя в другом регистре (`userid` — это другой параметр, `userId` отсутствует).

```shell
curl -i "$API/followers?userId=1" -w '\n'
curl -i "$API/followers" -w '\n'
curl -i "$API/followers?userId=" -w '\n'
curl -i "$API/followers?userId=%20" -w '\n'
curl -i "$API/followers?userid=$ALICE" -w '\n'
```

---

## 3. Проверка данных в БД

Значения `<alice>` и др. — из `echo` в разделе «Подготовка».

```shell
docker compose exec postgres psql -U postgres -d social_media
```

```sql
-- подписки Alice → Bob и Alice → Carol, новые первыми (как в ответе /api/v1/subscribes)
SELECT id, follower_id, followed_id, created_at
FROM subscriptions
WHERE follower_id = '<alice>'
ORDER BY created_at DESC, id DESC;

-- подписчики Bob (как в ответе /api/v1/subscribes/followers)
SELECT id, follower_id, created_at
FROM subscriptions
WHERE followed_id = '<bob>'
ORDER BY created_at DESC, id DESC;
```
