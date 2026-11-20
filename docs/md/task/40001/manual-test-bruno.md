# 40001 — ручная проверка в Bruno

Коллекция — `bruno/40001/` (открыть в Bruno: *Open Collection* → папка `bruno/40001`), окружение `local`.
Запросы выполняются по порядку (`seq` 1…12, *Run Collection*).

Пользователи генерируются заново в pre-request скрипте запроса `01` (`aliceId`, `borisId`, `vikaId`, `dimaId`),
поэтому коллекцию можно запускать повторно. `01` сохраняет `offerAlice`, `02` — `offerVika`.
Отдельный запрос из середины выполнять только после прогона `01`–`02`.

Сценарий: Алиса и Вика отправляют заявки Борису; Борис принимает заявку Алисы и отклоняет заявку Вики. Дима — посторонний.

Авторизации пока нет: вместо `Authorization: Bearer {{collection_dev_token}}` действующий пользователь
передаётся заголовком `X-User-Id` (Р0-1). Переменные окружения `local`:

| Переменная | Значение |
| --- | --- |
| `baseUrl` | `http://localhost:8080` |

Запуск приложения: `docker compose up -d postgres`, затем `./mvnw spring-boot:run`.

## Запросы

| № | Запрос | `X-User-Id` | Тело | Ожидается | Решение |
| --- | --- | --- | --- | --- | --- |
| 01 | `POST /friendship-offer/create` | Алиса | `{"toUserId":"{{borisId}}"}` | `201`, `fromUserId` = Алиса, `PENDING` | Р1-1, Р1-3 |
| 02 | `POST /friendship-offer/create` | Вика | `{"fromUserId":"{{dimaId}}","toUserId":"{{borisId}}"}` | `201`, `fromUserId` = Вика (поле тела игнорируется) | Р1-2 |
| 03 | `POST /friendship-offer/create` | — | `{"toUserId":"{{borisId}}"}` | `401 UNAUTHORIZED` | Р1-1 |
| 04 | `POST /friendship-offer/create` | Алиса | `{"toUserId":"{{aliceId}}"}` | `400 VALIDATION_ERROR` (самому себе) | Р1-6 |
| 05 | `POST /friendship-offer/create` | — | `{}` | `400 VALIDATION_ERROR` (тело проверяется раньше заголовка) | Р1-5 |
| 06 | `POST /friendship-offer/accept` | Алиса | `{"offerId":"{{offerAlice}}"}` | `404 FRIENDSHIP_OFFER_NOT_FOUND` (отправитель) | Р0-1 |
| 07 | `POST /friendship-offer/accept` | Дима | `{"offerId":"{{offerAlice}}","actorId":"{{borisId}}"}` | `404 FRIENDSHIP_OFFER_NOT_FOUND` (поле тела игнорируется) | Р1-2 |
| 08 | `POST /friendship-offer/accept` | — | `{"offerId":"{{offerAlice}}"}` | `401 UNAUTHORIZED` | Р1-1 |
| 09 | `POST /friendship-offer/accept` | Борис | `{"offerId":"{{offerAlice}}"}` | `200`, `ACCEPTED`, есть `acceptedAt` | Р1-3 |
| 10 | `POST /friendship-offer/reject` | — | `{"offerId":"{{offerVika}}"}` | `401 UNAUTHORIZED` | Р1-1 |
| 11 | `POST /friendship-offer/reject` | Дима | `{"offerId":"{{offerVika}}"}` | `404 FRIENDSHIP_OFFER_NOT_FOUND` | Р0-1 |
| 12 | `POST /friendship-offer/reject` | Борис | `{"offerId":"{{offerVika}}"}` | `200`, `REJECTED`, есть `rejectedAt` | Р1-3 |

Все пути — от `{{baseUrl}}/api/v1`.

## curl

```shell
# 01 Алиса предлагает дружбу Борису → 201
curl -i -X POST "{{baseUrl}}/api/v1/friendship-offer/create" \
  -H 'Content-Type: application/json' -H "X-User-Id: {{aliceId}}" \
  -d '{"toUserId":"{{borisId}}"}'

# 03 Без X-User-Id → 401
curl -i -X POST "{{baseUrl}}/api/v1/friendship-offer/create" \
  -H 'Content-Type: application/json' \
  -d '{"toUserId":"{{borisId}}"}'

# 09 Борис принимает заявку Алисы → 200
curl -i -X POST "{{baseUrl}}/api/v1/friendship-offer/accept" \
  -H 'Content-Type: application/json' -H "X-User-Id: {{borisId}}" \
  -d '{"offerId":"{{offerAlice}}"}'

# 12 Борис отклоняет заявку Вики → 200
curl -i -X POST "{{baseUrl}}/api/v1/friendship-offer/reject" \
  -H 'Content-Type: application/json' -H "X-User-Id: {{borisId}}" \
  -d '{"offerId":"{{offerVika}}"}'
```

Остальные запросы — те же curl с заголовком и телом из таблицы.

## Ожидаемые ответы

`01` — `201 Created`:

```json
{"id": "{{offerAlice}}", "fromUserId": "{{aliceId}}", "toUserId": "{{borisId}}", "status": "PENDING",
 "createdAt": "2026-11-20T10:15:00.123456Z"}
```

`09` — `200 OK`:

```json
{"id": "{{offerAlice}}", "fromUserId": "{{aliceId}}", "toUserId": "{{borisId}}", "status": "ACCEPTED",
 "createdAt": "2026-11-20T10:15:00.123456Z", "acceptedAt": "2026-11-20T10:15:03.654321Z"}
```

`12` — `200 OK`:

```json
{"id": "{{offerVika}}", "fromUserId": "{{vikaId}}", "toUserId": "{{borisId}}", "status": "REJECTED",
 "createdAt": "2026-11-20T10:15:01.123456Z", "rejectedAt": "2026-11-20T10:15:04.654321Z"}
```

`03`, `08`, `10` — `401 Unauthorized`:

```json
{"status": 401, "detail": "Пользователь не определён", "code": "UNAUTHORIZED"}
```

`04`, `05` — `400 Bad Request`:

```json
{"status": 400, "detail": "Некорректное тело или параметры запроса", "code": "VALIDATION_ERROR"}
```

`06`, `07`, `11` — `404 Not Found`:

```json
{"status": 404, "detail": "Предложение дружбы не найдено", "code": "FRIENDSHIP_OFFER_NOT_FOUND"}
```
