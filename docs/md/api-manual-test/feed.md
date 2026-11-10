# Проверка API ленты `/api/v1/feed`

Справка по эндпоинту `FeedController` и сценарий его ручной проверки на запущенном приложении.
Источник правды — контракт `docs/api/openapi.yaml` (путь `docs/api/paths/feed-get.yaml`, схема
`docs/api/schemas/post.yaml`); коды ошибок — `PostExceptionHandler` и `ErrorCode` модуля `posts`;
устройство ленты — `docs/process/Feed.md`. Один пост по id — в `post.md`.

Разделы эндпоинта: **Запрос**, **Ответ**, **Ошибки**, **Проверка** (команды `curl` и ожидаемый результат).
Команды выполняются сверху вниз в одном терминале (`zsh`/`bash`; нужны `curl`, `jq`, `uuidgen`).

Тело любой ошибки — `{"code": "...", "message": "..."}` (схема `docs/api/schemas/error.yaml`):

| `code` | Статус | `message` |
| --- | --- | --- |
| `INVALID_POST_REQUEST` | `400` | Некорректные параметры запроса |

Ограничения запроса — только из контракта (`docs/api/paths/feed-get.yaml`), в сгенерированном `FeedApi`:

| Параметр | Контракт | `FeedApi` |
| --- | --- | --- |
| `followerId` | `required: true`, `format: uuid` | `@NotNull @RequestParam(required = true) UUID` |
| `limit` | `integer`, `minimum: 1`, `maximum: 100`, `default: 20` | `@Min(1) @Max(100) @RequestParam(defaultValue = "20") Integer` |
| `afterCreatedAt` | `format: date-time` | `@DateTimeFormat(iso = DATE_TIME) Instant` |
| `afterId` | `format: uuid` | `UUID` |

Правило «`afterCreatedAt` и `afterId` вместе или ни одного» в OpenAPI не выражается: его проверяет сценарий
`GetFeed` (`IncompleteFeedCursorException`). Тела запроса нет.

| Эндпоинт | Назначение |
| --- | --- |
| [`GET /api/v1/feed`](#1-get-apiv1feed) | Страница ленты читателя |

## Подготовка

БД поднимается на `localhost:5435`, приложение — на `http://localhost:8080` (в отдельном терминале).

```shell
docker compose up -d postgres
./mvnw spring-boot:run
```

Reader подписывается на Alice и Bob заявками в друзья (отправитель заявки подписывается на получателя).
Посты вставляются в БД напрямую (REST API создания постов нет): у Alice — три опубликованных и один удалённый,
у Bob — один, у Stranger (на него Reader не подписан) — один. Время постов разное, порядок ленты известен заранее.
Id каждый раз новые, сценарий можно повторять.

```shell
API=http://localhost:8080/api/v1/feed
OFFERS=http://localhost:8080/api/v1/friendship-offer
PSQL='docker compose exec -T postgres psql -U postgres -d social_media -v ON_ERROR_STOP=1 -q'
new() { uuidgen | tr 'A-Z' 'a-z'; }
READER=$(new); ALICE=$(new); BOB=$(new); STRANGER=$(new)
A1=$(new); A2=$(new); A3=$(new); ADEL=$(new); B1=$(new); S1=$(new)
echo "reader=$READER alice=$ALICE bob=$BOB stranger=$STRANGER"

for AUTHOR in $ALICE $BOB; do
  curl -s -o /dev/null -w '%{http_code}\n' -X POST "$OFFERS/create" -H 'Content-Type: application/json' \
    -d "{\"fromUserId\":\"$READER\",\"toUserId\":\"$AUTHOR\"}"
done

$PSQL -c "INSERT INTO posts (id, author_id, title, content, status, created_at, updated_at) VALUES
  ('$A1',   '$ALICE',    'A1',   'Текст', 'PUBLISHED', '2026-10-15T10:00:00Z', '2026-10-15T10:00:00Z'),
  ('$B1',   '$BOB',      'B1',   'Текст', 'PUBLISHED', '2026-10-15T10:01:00Z', '2026-10-15T10:01:00Z'),
  ('$A2',   '$ALICE',    'A2',   'Текст', 'PUBLISHED', '2026-10-15T10:02:00Z', '2026-10-15T10:02:00Z'),
  ('$ADEL', '$ALICE',    'ADEL', 'Текст', 'DELETED',   '2026-10-15T10:03:00Z', '2026-10-15T10:04:00Z'),
  ('$S1',   '$STRANGER', 'S1',   'Текст', 'PUBLISHED', '2026-10-15T10:05:00Z', '2026-10-15T10:05:00Z'),
  ('$A3',   '$ALICE',    'A3',   'Текст', 'PUBLISHED', '2026-10-15T10:06:00Z', '2026-10-15T10:06:00Z');"
```

Ожидаемо: `201` и `201`. Лента Reader — `A3, A2, B1, A1` (без `ADEL` — удалён, без `S1` — нет подписки).

---

## 1. `GET /api/v1/feed`

Опубликованные посты авторов, на которых подписан читатель, от новых к старым (`createdAt DESC, id DESC`).
Страницы — по курсору: первая без `afterCreatedAt`/`afterId`, следующая — с `afterCreatedAt = next.createdAt`
и `afterId = next.id`; `next: null` — постов больше нет. `OFFSET` нет.

### Запрос

| Параметр | Где | Тип | Обязательный | Описание |
| --- | --- | --- | --- | --- |
| `followerId` | query | `string (uuid)` | Да | Читатель (до подключения Keycloak передаётся клиентом) |
| `limit` | query | `integer` 1..100 | Нет, `20` | Размер страницы |
| `afterCreatedAt` | query | `string (date-time)` | Нет | `next.createdAt` предыдущей страницы, вместе с `afterId` |
| `afterId` | query | `string (uuid)` | Нет | `next.id` предыдущей страницы, вместе с `afterCreatedAt` |

### Ответ

`200 OK`; пустая лента — `{"posts": [], "next": null}`.

```json
{
  "posts": [
    {
      "id": "7d1f3c2a-5b6e-4f80-9a1b-2c3d4e5f6a7b",
      "authorId": "22222222-2222-2222-2222-222222222222",
      "title": "A3",
      "content": "Текст",
      "status": "PUBLISHED",
      "createdAt": "2026-10-15T10:06:00Z",
      "updatedAt": "2026-10-15T10:06:00Z"
    }
  ],
  "next": {"createdAt": "2026-10-15T10:06:00Z", "id": "7d1f3c2a-5b6e-4f80-9a1b-2c3d4e5f6a7b"}
}
```

### Ошибки

| Статус | `code` | Когда |
| --- | --- | --- |
| `400` | `INVALID_POST_REQUEST` | Нет `followerId`; `followerId` / `afterId` не UUID; `limit` вне 1..100 или не число; `afterCreatedAt` не дата-время; задана только одна часть курсора |

### Проверка

**1.1. Вся лента** → `200`, `A3, A2, B1, A1`, `next` = `null`.

```shell
curl -s "$API?followerId=$READER" | jq -r '.posts[].title, .next'
```

**1.2. Страницы по 2** → первая `A3, A2` и `next` на `A2` (`true`); вторая (по курсору из `next`) —
`B1, A1` и `next` = `null`: постов больше нет, третьей страницы не бывает.

```shell
PAGE=$(curl -s "$API?followerId=$READER&limit=2"); echo "$PAGE" | jq -r '.posts[].title, .next.id == "'$A2'"'
NEXT_AT=$(echo "$PAGE" | jq -r '.next.createdAt'); NEXT_ID=$(echo "$PAGE" | jq -r '.next.id')
curl -s "$API?followerId=$READER&limit=2&afterCreatedAt=$NEXT_AT&afterId=$NEXT_ID" | jq -r '.posts[].title, .next'
```

**1.3. Читатель без подписок** → `200`, `{"posts":[],"next":null}`.

```shell
curl -s "$API?followerId=$STRANGER" -w '\n'
```

**1.4. Некорректные параметры** → каждый `400 INVALID_POST_REQUEST`.

```shell
curl -s "$API" -w '\n'                                                     # нет followerId
curl -s "$API?followerId=42" -w '\n'                                       # не UUID
curl -s "$API?followerId=$READER&limit=0" -w '\n'                          # limit < 1
curl -s "$API?followerId=$READER&limit=101" -w '\n'                        # limit > 100
curl -s "$API?followerId=$READER&limit=abc" -w '\n'                        # limit не число
curl -s "$API?followerId=$READER&afterId=$A1" -w '\n'                      # только afterId
curl -s "$API?followerId=$READER&afterCreatedAt=2026-10-15T10:00:00Z" -w '\n'  # только afterCreatedAt
curl -s "$API?followerId=$READER&afterCreatedAt=yesterday&afterId=$A1" -w '\n' # не дата-время
curl -s "$API?followerId=" -w '\n'                                        # пустой followerId
curl -s "$API?followerid=$READER" -w '\n'                                 # имя в другом регистре
curl -s "$API?followerId=$READER&limit=-1" -w '\n'                        # limit < 1
curl -s "$API?followerId=$READER&limit=1.5" -w '\n'                       # limit не целое
curl -s "$API?followerId=$READER&afterCreatedAt=2026-10-15T10:00:00&afterId=$A1" -w '\n'  # без часового пояса
curl -s "$API?followerId=$READER&afterCreatedAt=2026-10-15&afterId=$A1" -w '\n'           # только дата
```

**1.5. Допустимые граничные значения** → `200`: пустой `limit` — значение по умолчанию (20), время со смещением
(`+05:00`, в URL `%2B05:00`) — тот же момент, что и в UTC (ответ как в 1.2 для второй страницы: `B1, A1`).

```shell
curl -s "$API?followerId=$READER&limit=" | jq -r '.posts[].title'
curl -s "$API?followerId=$READER&afterCreatedAt=2026-10-15T15:02:00%2B05:00&afterId=$A2" | jq -r '.posts[].title'
```

---

## 2. Проверка данных в БД

Значения `<reader>` и др. — из `echo` в разделе «Подготовка».

```shell
docker compose exec postgres psql -U postgres -d social_media
```

```sql
-- подписки Reader: Alice и Bob
SELECT follower_id, followed_id FROM subscriptions WHERE follower_id = '<reader>';

-- что попадёт в ленту: опубликованные посты авторов из подписок, новые первыми (как в ответе 1.1)
SELECT p.title, p.author_id, p.status, p.created_at
FROM posts p
JOIN subscriptions s ON s.followed_id = p.author_id
WHERE s.follower_id = '<reader>' AND p.status = 'PUBLISHED'
ORDER BY p.created_at DESC, p.id DESC;
```
