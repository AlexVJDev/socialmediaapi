# Проверка API постов `/api/v1/posts`

Справка по эндпоинтам `PostController` и сценарий их ручной проверки на запущенном приложении.
Источник правды — контракт `docs/api/openapi.yaml` (путь `docs/api/paths/post-get.yaml`, схема
`docs/api/schemas/post.yaml`); коды ошибок — `PostExceptionHandler` и `ErrorCode` модуля `posts`.
Лента (`FeedController`) — в `feed.md`.

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
| `INVALID_POST_REQUEST` | `400` | Некорректные параметры запроса |
| `POST_NOT_FOUND` | `404` | Пост не найден |

Ограничения запроса — только из контракта: один path-параметр `postId` (`required: true`, `format: uuid`),
query-параметров и тела нет. В сгенерированном `PostApi` это `@PathVariable UUID postId`.

| Эндпоинт | Назначение |
| --- | --- |
| [`GET /api/v1/posts/{postId}`](#1-get-apiv1postspostid) | Опубликованный пост по id |

## Подготовка

БД поднимается на `localhost:5435`, приложение — на `http://localhost:8080` (в отдельном терминале).

```shell
docker compose up -d postgres
./mvnw spring-boot:run
```

REST API для создания и удаления постов нет: посты вставляются в БД напрямую. Создаются опубликованный пост и
удалённый (`DELETED`, удаление логическое — строка остаётся). Id каждый раз новые, сценарий можно повторять.

```shell
API=http://localhost:8080/api/v1/posts
PSQL='docker compose exec -T postgres psql -U postgres -d social_media -v ON_ERROR_STOP=1 -q'
AUTHOR=$(uuidgen | tr 'A-Z' 'a-z')
POST=$(uuidgen | tr 'A-Z' 'a-z')
DELETED=$(uuidgen | tr 'A-Z' 'a-z')
echo "author=$AUTHOR post=$POST deleted=$DELETED"

$PSQL -c "INSERT INTO posts (id, author_id, title, content, status, created_at, updated_at) VALUES
  ('$POST',    '$AUTHOR', 'Пост 1', 'Текст поста', 'PUBLISHED', '2026-10-15T10:00:00Z', '2026-10-15T10:00:00Z'),
  ('$DELETED', '$AUTHOR', 'Пост 2', 'Удалён',      'DELETED',   '2026-10-15T11:00:00Z', '2026-10-15T12:00:00Z');"
```

---

## 1. `GET /api/v1/posts/{postId}`

Опубликованный пост. Удалённый пост читается как отсутствующий — для автора тоже.

### Запрос

| Параметр | Где | Тип | Обязательный | Описание |
| --- | --- | --- | --- | --- |
| `postId` | path | `string (uuid)` | Да | Id поста |

### Ответ

`200 OK`. `status` всегда `PUBLISHED`.

```json
{
  "id": "7d1f3c2a-5b6e-4f80-9a1b-2c3d4e5f6a7b",
  "authorId": "22222222-2222-2222-2222-222222222222",
  "title": "Пост 1",
  "content": "Текст поста",
  "status": "PUBLISHED",
  "createdAt": "2026-10-15T10:00:00Z",
  "updatedAt": "2026-10-15T10:00:00Z"
}
```

### Ошибки

| Статус | `code` | Когда |
| --- | --- | --- |
| `400` | `INVALID_POST_REQUEST` | `postId` не UUID или пуст (пробел) |
| `404` | `POST_NOT_FOUND` | Поста нет или он удалён |

### Проверка

**1.1. Опубликованный пост** → `200`, поля как в `INSERT`, `status` = `PUBLISHED`.

```shell
curl -s "$API/$POST" | jq
```

**1.2. Удалённый пост** → `404 POST_NOT_FOUND`.

```shell
curl -i "$API/$DELETED" -w '\n'
```

**1.3. Несуществующий пост** → `404 POST_NOT_FOUND`.

```shell
curl -i "$API/$(uuidgen | tr 'A-Z' 'a-z')" -w '\n'
```

**1.4. Некорректный `postId`** → оба `400 INVALID_POST_REQUEST`: не UUID, пробел.

```shell
curl -i "$API/42" -w '\n'
curl -i "$API/%20" -w '\n'
```

---

## 2. Проверка данных в БД

```shell
docker compose exec postgres psql -U postgres -d social_media
```

```sql
-- оба поста на месте: удаление логическое, DELETED не отдаётся API
SELECT id, author_id, title, status, created_at, updated_at
FROM posts
WHERE author_id = '<author>';
```
