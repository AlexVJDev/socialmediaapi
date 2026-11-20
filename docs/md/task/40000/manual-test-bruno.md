# 40000 — ручная проверка в Bruno

Коллекция — `bruno/40000/` (открыть в Bruno: *Open Collection* → папка `bruno/40000`), окружение `local`.
Запросы выполняются по порядку (`seq` 1…15, *Run Collection*).

Пользователи генерируются заново в pre-request скрипте запроса `01` (`aliceId`, `borisId`, `vikaId`, `dimaId`),
поэтому коллекцию можно запускать повторно. Запросы сохраняют: `01`–`03` — `offerAlice`, `offerVika`, `offerDima`;
`06` — `nextCreatedAt`, `nextId`. Отдельный запрос из середины выполнять только после прогона `01`–`04`.

Сценарий: Алиса, Вика, Дима отправляют заявки Борису (в этом порядке), Борис принимает заявку Вики.

Авторизации пока нет: вместо `Authorization: Bearer {{collection_dev_token}}` текущий пользователь
передаётся заголовком `X-User-Id` (Р3-2). Переменные окружения `local`:

| Переменная | Значение |
| --- | --- |
| `baseUrl` | `http://localhost:8080` |

Запуск приложения: `docker compose up -d postgres`, затем `./mvnw spring-boot:run`.

## Запросы

| № | Запрос | `X-User-Id` | Тело | Ожидается |
| --- | --- | --- | --- | --- |
| 01 | `POST /api/v1/friendship-offer/create` | Алиса | `{"toUserId":"{{borisId}}"}` | `201` |
| 02 | `POST /api/v1/friendship-offer/create` | Вика | `{"toUserId":"{{borisId}}"}` | `201` |
| 03 | `POST /api/v1/friendship-offer/create` | Дима | `{"toUserId":"{{borisId}}"}` | `201` |
| 04 | `POST /api/v1/friendship-offer/accept` | Борис | `{"offerId":"{{offerVika}}"}` | `200` |
| 05 | `GET /api/v1/friendship-offer?direction=INCOMING` | Борис | — | `200`, `offers` = [Дима, Алиса], `next: null` |
| 06 | `GET /api/v1/friendship-offer?direction=INCOMING&limit=1` | Борис | — | `200`, `offers` = [Дима], `next` = позиция заявки Димы |
| 07 | `GET /api/v1/friendship-offer?direction=INCOMING&limit=1&afterCreatedAt={{nextCreatedAt}}&afterId={{nextId}}` | Борис | — | `200`, `offers` = [Алиса], `next: null` |
| 08 | `GET /api/v1/friendship-offer?direction=INCOMING&status=ACCEPTED` | Борис | — | `200`, `offers` = [Вика] |
| 09 | `GET /api/v1/friendship-offer?direction=OUTGOING` | Алиса | — | `200`, `offers` = [заявка Алисы Борису] |
| 10 | `GET /api/v1/friendship-offer` | Борис | — | `400 VALIDATION_ERROR` |
| 11 | `GET /api/v1/friendship-offer?direction=FOO` | Борис | — | `400 VALIDATION_ERROR` |
| 12 | `GET /api/v1/friendship-offer?direction=INCOMING` | — | — | `401 UNAUTHORIZED` |
| 13 | `GET /api/v1/friendships?userId={{borisId}}` | — | — | `200`, `friends` = [Вика], `next: null` |
| 14 | `GET /api/v1/friendships?userId={{vikaId}}` | — | — | `200`, `friends` = [Борис], тот же `id` дружбы |
| 15 | `GET /api/v1/friendships?userId=abc` | — | — | `400 VALIDATION_ERROR` |

Пример ответа `05`:

```json
{
  "offers": [
    {"id": "{{offerDima}}", "fromUserId": "{{dimaId}}", "toUserId": "{{borisId}}", "status": "PENDING",
     "createdAt": "2026-11-20T10:15:00.123456Z", "updatedAt": "2026-11-20T10:15:00.123456Z"},
    {"id": "{{offerAlice}}", "fromUserId": "{{aliceId}}", "toUserId": "{{borisId}}", "status": "PENDING",
     "createdAt": "2026-11-20T10:14:59.654321Z", "updatedAt": "2026-11-20T10:14:59.654321Z"}
  ],
  "next": null
}
```

Пример ответа `13`:

```json
{
  "friends": [
    {"id": "c1d2e3f4-5a6b-4c7d-8e9f-0a1b2c3d4e02", "userId": "{{vikaId}}"}
  ],
  "next": null
}
```

Ответ ошибки — `application/problem+json`:
`{"status": 400, "detail": "Некорректное тело или параметры запроса", "code": "VALIDATION_ERROR"}`,
`{"status": 401, "detail": "Пользователь не определён", "code": "UNAUTHORIZED"}`.

## curl для каждого запроса

Переменные Bruno подставить вручную: `{{baseUrl}}`, id пользователей (любые новые UUID),
`{{offerVika}}` (из ответа 02), `{{nextCreatedAt}}`/`{{nextId}}` (из `next` ответа 06).

```shell
# 01, 02, 03 — X-User-Id: Алиса, Вика, Дима (задача 40001)
curl -i -X POST '{{baseUrl}}/api/v1/friendship-offer/create' -H 'Content-Type: application/json' \
  -H 'X-User-Id: {{aliceId}}' -d '{"toUserId":"{{borisId}}"}'
# 04
curl -i -X POST '{{baseUrl}}/api/v1/friendship-offer/accept' -H 'Content-Type: application/json' \
  -H 'X-User-Id: {{borisId}}' -d '{"offerId":"{{offerVika}}"}'
# 05
curl -i '{{baseUrl}}/api/v1/friendship-offer?direction=INCOMING' -H 'X-User-Id: {{borisId}}'
# 06
curl -i '{{baseUrl}}/api/v1/friendship-offer?direction=INCOMING&limit=1' -H 'X-User-Id: {{borisId}}'
# 07
curl -i -G '{{baseUrl}}/api/v1/friendship-offer' -H 'X-User-Id: {{borisId}}' \
  --data-urlencode 'direction=INCOMING' --data-urlencode 'limit=1' \
  --data-urlencode 'afterCreatedAt={{nextCreatedAt}}' --data-urlencode 'afterId={{nextId}}'
# 08
curl -i '{{baseUrl}}/api/v1/friendship-offer?direction=INCOMING&status=ACCEPTED' -H 'X-User-Id: {{borisId}}'
# 09
curl -i '{{baseUrl}}/api/v1/friendship-offer?direction=OUTGOING' -H 'X-User-Id: {{aliceId}}'
# 10
curl -i '{{baseUrl}}/api/v1/friendship-offer' -H 'X-User-Id: {{borisId}}'
# 11
curl -i '{{baseUrl}}/api/v1/friendship-offer?direction=FOO' -H 'X-User-Id: {{borisId}}'
# 12
curl -i '{{baseUrl}}/api/v1/friendship-offer?direction=INCOMING'
# 13, 14
curl -i '{{baseUrl}}/api/v1/friendships?userId={{borisId}}'
# 15
curl -i '{{baseUrl}}/api/v1/friendships?userId=abc'
```
