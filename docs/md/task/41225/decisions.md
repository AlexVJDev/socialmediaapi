# 41225 — журнал решений

| ID | Решение (1–2 строки) | Источник | Статус |
|---|---|---|---|
| Р0-1 | Режим ментора: план — один `task-manual.md` для пользователя, код пишет пользователь | fix.md, 15.11.2026 | действует |
| Р1-1 | Параметр `followerId` ленты не убирать. `CurrentUserProvider` необязателен; если вводится — с фиксированным ID по умолчанию (детали — Раунд 2, в.1) | fix.md Раунд 1, в.1 | заменено Р0-2 (followerId — Раунд 3, в.3) |
| Р1-2 | Префикс `/api/v1`: `/api/v1/posts`, `/api/v1/posts/{postId}`, `/api/v1/users/{userId}/posts`, `/api/v1/feed` | fix.md Раунд 1, в.2 | действует |
| Р1-3 | Плоские path-файлы: `post-create.yaml`, `post-by-id.yaml` (GET/PUT/DELETE; `post-get.yaml` переименовать), `posts-by-user.yaml`, `feed-get.yaml` | fix.md Раунд 1, в.3 | действует |
| Р1-4 | Пагинация — курсор как в ленте (`limit/afterCreatedAt/afterId` → `{posts, next}`), и для `GET /users/{userId}/posts`; `PageResponse` не вводить | fix.md Раунд 1, в.4 | действует |
| Р1-5 | В subscriptions добавить `POST /api/v1/subscribes` и `DELETE /api/v1/subscribes/{userId}` — отдельная подзадача; сценарный тест целиком через MockMvc | fix.md Раунд 1, в.5 | действует |
| Р1-6 | Доменная модель `Post`: `update(actorId, …)` / `delete(actorId)` проверяют автора и статус; use case: чтение → домен → сохранение; `WHERE author_id/status` в SQL остаётся защитой от гонок | fix.md Раунд 1, в.6 | действует |
| Р1-7 | Чужой пост в PUT/DELETE → `404 POST_NOT_FOUND`, `403` не вводить | fix.md Раунд 1, в.7 | действует |
| Р1-8 | Одна схема `post-request.yaml` для POST и PUT: `title` 1..255, `content` 1..10000, оба обязательны, не только пробелы; ошибка → `400 VALIDATION_ERROR` | fix.md Раунд 1, в.8 | действует |
| Р1-9 | `imageIds` не добавлять | fix.md Раунд 1, в.9 | действует |
| Р2-1 | Текущий пользователь в итоге берётся из Keycloak (`CurrentUserProvider`); до этого для ручных и тестовых запросов клиент может прислать произвольный ID в JSON, без него — `app.current-user.default-id` (детали — Раунд 3) | fix.md Раунд 2, в.1 | заменено Р0-2 |
| Р2-2 | `POST/DELETE /api/v1/subscribes`: подписчик определяется так же, как в Р2-1 | fix.md Раунд 2, в.2 | заменено Р0-3 |
| Р2-3 | Подписка: `201` + подписка; повторная → `200` + существующая (`subscribeIfAbsent`); на себя → `400 VALIDATION_ERROR`. Отписка: всегда `204` | fix.md Раунд 2, в.3 | действует |
| Р1-10 | Ответы: POST → `201` + `Location` + пост; PUT → `200` + пост; DELETE → `204`, повторный → `404`; посты неизвестного пользователя → `200` с пустой страницей | fix.md Раунд 1, к сведению | действует |
| Р0-2 | `app.current-user.default-id` и поле ID в JSON/query не заводить; текущий пользователь — только из `CurrentUserProvider` (реализация — Keycloak, JWT); автор поста от клиента не принимается | чат, 15.11.2026 | заменено Р0-4 |
| Р0-3 | `POST/DELETE /api/v1/subscribes`: подписчик — из `CurrentUserProvider` | чат, 15.11.2026 | действует |
| Р0-4 | Только порт `CurrentUserProvider`, Keycloak/Spring Security в задаче не подключать; `app.current-user.default-id` и ID в JSON/query не заводить; автор поста от клиента не принимается | чат, 15.11.2026 | действует |
| Р3-1 | Временная реализация порта — `HeaderCurrentUserProvider`: заголовок `X-User-Id` (UUID); нет или не UUID → `401 UNAUTHORIZED`; в DTO/тела не попадает | fix.md Раунд 3, в.1 | действует |
| Р3-2 | Порт `CurrentUserProvider` и `HeaderCurrentUserProvider` — в `shared.security`; `CommonErrorCode.UNAUTHORIZED` + `UnauthenticatedException` → `401` в `GlobalExceptionHandler` | fix.md Раунд 3, в.2 | действует |
| Р3-3 | `followerId` из ленты убрать: лента текущего пользователя из порта | fix.md Раунд 3, в.3 | действует |
| Р3-4 | Порт используют: POST/PUT/DELETE постов, `GET /feed`, POST/DELETE `/subscribes`; friendships и GET-списки subscriptions не трогать. В контракте — `securitySchemes` apiKey `X-User-Id`; сценарный тест передаёт заголовок, без `@MockitoBean` | fix.md Раунд 3, к сведению | действует |
| Р0-5 | keyLock не используем: гонки закрываются атомарным SQL (`UPDATE ... WHERE author_id/status` — Р1-6, `createSubscriptionIfAbsent` — Р2-3) | чат, 16.11.2026 | действует |
| Р0-6 | Этап 1 плана выполняет ИИ (исключение из Р0-1 для этого этапа) | чат, 16.11.2026 | действует |
| Р0-7 | `UnauthenticatedException` остаётся в `shared.security` рядом с портом `CurrentUserProvider` (часть его контракта), без подпапки `exception` | чат, 16.11.2026 | действует |
| Р0-8 | Этап 2 плана выполняет ИИ | чат, 16.11.2026 | действует |
| Р0-9 | После этапа 2 сборка остаётся некомпилируемой до этапа 4 (`PostWebMapper`, `PostController`, `FeedController`); заглушки 501 не делаем, «verify зелёный после каждого этапа» для этапов 2–3 не действует | чат, 16.11.2026 | действует |
| Р0-10 | Javadoc нового кода задачи — на русском (в т.ч. перевести Javadoc этапа 1); существующий английский Javadoc не трогаем | чат, 16.11.2026 | действует |
| Р0-11 | Упоминания Keycloak в Javadoc и `openapi.yaml` оставляем | чат, 16.11.2026 | действует |
| Р0-12 | Этап 3 плана выполняет ИИ | чат, 16.11.2026 | действует |
| Р0-13 | Модели постов не объединять: команды на каждый сценарий, `PostResult`, `PostResponse`, `FeedPostResponse`, `CreatePostRequest` остаются — как в friendships | чат, 16.11.2026 | действует |
| Р0-14 | Этап 4 плана выполняет ИИ | чат, 16.11.2026 | действует |
| Р0-15 | Этап 5 плана выполняет ИИ | чат, 16.11.2026 | действует |
| Р0-16 | Этап 6 плана выполняет ИИ | чат, 16.11.2026 | действует |
| Р0-17 | Этап 7 плана выполняет ИИ | чат, 16.11.2026 | действует |
| Р0-18 | `X-User-Id` принимается только как UUID в каноническом виде (иначе `401`) | code-review.md п.3, 16.11.2026 | действует |
| Р0-19 | По код-ревью: п.1 (повтор подписки при гонке) пока не трогаем; п.2, 6, 9 — оставить; п.5 (`architecture.md`) — только по команде; п.8 — отдельной задачей | code-review.md, 16.11.2026 | действует |
