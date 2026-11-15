# 41225 — заметки анализа

Урок: «API публикаций и ленты» (#505789), описание — `Descr.pdf`.

## Что уже есть (на 15.11.2026, коммит 1e54260)
- Таблица `posts` (007/008), статусы `PUBLISHED`/`DELETED`.
- Порт `PostRepository`: `createPost`, `findPublishedById`, `updatePost`, `deletePost`; автор и статус проверяются в `WHERE` одного `UPDATE`.
- REST: `GET /api/v1/posts/{postId}` (`PostController`), `GET /api/v1/feed` (`FeedController`, курсор, `followerId` в query).
- Генерация: `docs/api/modules/posts.yaml`, executions `posts-api`, `posts-responses` (без `posts-requests`: тел запросов не было).
- Ошибки: `PostExceptionHandler`, `PostErrorCode.POST_NOT_FOUND`, `CommonErrorCode.VALIDATION_ERROR`.
- `Subscriptions` (api): `subscribeIfAbsent`, `unsubscribe`; HTTP — только чтение списков.
- `FriendshipFeedTest`: дружба → взаимные подписки → лента (посты через порт).

## Чего нет
- `CurrentUserProvider`.
- Use case'ов create/update/delete поста, доменной модели `Post`.
- `GET /users/{userId}/posts` (и запроса в адаптере).
- HTTP подписки/отписки.
- Execution `posts-requests` (появится тело `post-request`).

## Расхождения урока с проектом
Префикс `/api` vs `/api/v1`; вложенные path-файлы vs плоские; `page/size` + `PageResponse` vs курсор;
`CurrentUserProvider` vs id от клиента. Вынесено в fix.md, Раунд 1.
