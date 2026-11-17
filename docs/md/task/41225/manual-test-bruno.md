# 41225 — ручная проверка в Bruno

Коллекция — `bruno/41225/` (открыть в Bruno: *Open Collection* → папка `bruno/41225`), окружение `local`.
Запросы выполняются по порядку (`seq` 1…17, *Run Collection*): `02` сохраняет `postId`, `08` — `subscriptionId`.
Коллекцию можно запускать повторно: в конце Алиса отписана, пост удалён.

**Если прогон прервался** между `08` и `13`, Алиса осталась подписана на Бориса, и при следующем
запуске `08` вернёт `200` вместо `201`. Перед повторным запуском выполните вручную запрос
`13 Алиса отписывается от Бориса` или поменяйте `aliceId`/`borisId` в окружении `local` на новые UUID.

Авторизации пока нет (Р0-4): вместо `Authorization: Bearer {{collection_dev_token}}` текущий пользователь
передаётся заголовком `X-User-Id` (Р3-1). Переменные окружения:

| Переменная | Значение |
| --- | --- |
| `baseUrl` | `http://localhost:8080` |
| `aliceId` | `11111111-4122-4500-8000-000000000001` |
| `borisId` | `11111111-4122-4500-8000-000000000002` |

Запуск приложения: `docker compose up -d postgres`, затем `./mvnw spring-boot:run`.

## Запросы

| № | Запрос | `X-User-Id` | Тело | Ожидается |
| --- | --- | --- | --- | --- |
| 01 | `POST /api/v1/posts` | — | `{"title":"Пост","content":"Текст"}` | `401 UNAUTHORIZED` |
| 02 | `POST /api/v1/posts` | Борис | `{"title":"Пост Бориса","content":"Текст Бориса"}` | `201`, `Location: /api/v1/posts/{id}`, `authorId` = Борис |
| 03 | `POST /api/v1/posts` | Борис | `{"title":"   ","content":"Текст"}` | `400 VALIDATION_ERROR` |
| 04 | `GET /api/v1/posts/{{postId}}` | — | — | `200`, пост |
| 05 | `PUT /api/v1/posts/{{postId}}` | Алиса | `{"title":"Чужой","content":"Чужой"}` | `404 POST_NOT_FOUND` |
| 06 | `PUT /api/v1/posts/{{postId}}` | Борис | `{"title":"Новый заголовок","content":"Новый текст"}` | `200`, новый `title` |
| 07 | `GET /api/v1/users/{{borisId}}/posts?limit=20` | — | — | `200`, пост в `posts` |
| 08 | `POST /api/v1/subscribes` | Алиса | `{"userId":"{{borisId}}"}` | `201`, подписка |
| 09 | `POST /api/v1/subscribes` | Алиса | `{"userId":"{{borisId}}"}` | `200`, та же подписка |
| 10 | `POST /api/v1/subscribes` | Алиса | `{"userId":"{{aliceId}}"}` | `400 VALIDATION_ERROR` |
| 11 | `GET /api/v1/feed` | Алиса | — | `200`, пост Бориса в `posts` |
| 12 | `GET /api/v1/feed` | — | — | `401 UNAUTHORIZED` |
| 13 | `DELETE /api/v1/subscribes/{{borisId}}` | Алиса | — | `204` |
| 14 | `GET /api/v1/feed` | Алиса | — | `200`, поста Бориса нет |
| 15 | `DELETE /api/v1/posts/{{postId}}` | Алиса | — | `404 POST_NOT_FOUND` |
| 16 | `DELETE /api/v1/posts/{{postId}}` | Борис | — | `204` |
| 17 | `GET /api/v1/posts/{{postId}}` | — | — | `404 POST_NOT_FOUND` |

Ответ ошибки — `application/problem+json`: `{"status": 401, "detail": "Пользователь не определён", "code": "UNAUTHORIZED"}`.

## curl для каждого запроса

Переменные Bruno подставить вручную: `{{baseUrl}}`, `{{aliceId}}`, `{{borisId}}`, `{{postId}}` (из ответа 02).

```shell
# 01
curl -i -X POST '{{baseUrl}}/api/v1/posts' -H 'Content-Type: application/json' -d '{"title":"Пост","content":"Текст"}'
# 02
curl -i -X POST '{{baseUrl}}/api/v1/posts' -H 'X-User-Id: {{borisId}}' -H 'Content-Type: application/json' \
  -d '{"title":"Пост Бориса","content":"Текст Бориса"}'
# 03
curl -i -X POST '{{baseUrl}}/api/v1/posts' -H 'X-User-Id: {{borisId}}' -H 'Content-Type: application/json' \
  -d '{"title":"   ","content":"Текст"}'
# 04
curl -i '{{baseUrl}}/api/v1/posts/{{postId}}'
# 05
curl -i -X PUT '{{baseUrl}}/api/v1/posts/{{postId}}' -H 'X-User-Id: {{aliceId}}' -H 'Content-Type: application/json' \
  -d '{"title":"Чужой","content":"Чужой"}'
# 06
curl -i -X PUT '{{baseUrl}}/api/v1/posts/{{postId}}' -H 'X-User-Id: {{borisId}}' -H 'Content-Type: application/json' \
  -d '{"title":"Новый заголовок","content":"Новый текст"}'
# 07
curl -i '{{baseUrl}}/api/v1/users/{{borisId}}/posts?limit=20'
# 08, 09 (повторно)
curl -i -X POST '{{baseUrl}}/api/v1/subscribes' -H 'X-User-Id: {{aliceId}}' -H 'Content-Type: application/json' \
  -d '{"userId":"{{borisId}}"}'
# 10
curl -i -X POST '{{baseUrl}}/api/v1/subscribes' -H 'X-User-Id: {{aliceId}}' -H 'Content-Type: application/json' \
  -d '{"userId":"{{aliceId}}"}'
# 11, 14
curl -i '{{baseUrl}}/api/v1/feed' -H 'X-User-Id: {{aliceId}}'
# 12
curl -i '{{baseUrl}}/api/v1/feed'
# 13
curl -i -X DELETE '{{baseUrl}}/api/v1/subscribes/{{borisId}}' -H 'X-User-Id: {{aliceId}}'
# 15
curl -i -X DELETE '{{baseUrl}}/api/v1/posts/{{postId}}' -H 'X-User-Id: {{aliceId}}'
# 16
curl -i -X DELETE '{{baseUrl}}/api/v1/posts/{{postId}}' -H 'X-User-Id: {{borisId}}'
# 17
curl -i '{{baseUrl}}/api/v1/posts/{{postId}}'
```

Пример ответа 02:

```json
{
  "id": "7d1f3c2a-5b6e-4f80-9a1b-2c3d4e5f6a7b",
  "authorId": "11111111-4122-4500-8000-000000000002",
  "title": "Пост Бориса",
  "content": "Текст Бориса",
  "status": "PUBLISHED",
  "createdAt": "2026-11-16T10:00:00.123456Z",
  "updatedAt": "2026-11-16T10:00:00.123456Z"
}
```

Пример ответа 08: `{"id": "<id подписки>", "userId": "11111111-4122-4500-8000-000000000002"}`.
