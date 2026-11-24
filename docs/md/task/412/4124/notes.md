# 4124 — заметки анализа

Урок: `FriendshipEntity`, `SubscriptionEntity`, репозитории на `JpaRepository`, запросы списка друзей
(JPQL `case when … or …`, `Slice<UUID>` + `Pageable`) и подписчиков (`findByFollowedIdOrderByCreatedAtDescIdDesc`,
`Slice` + `Pageable`); по одному `@DataJpaTest` на репозиторий.

## Порты → JPA
### FriendshipsRepository (friendships)
| Метод | Сейчас (JDBC) | JPA |
|---|---|---|
| `createFriendship` | `INSERT … RETURNING`, дубль по `uq_friendships_users` → `FriendshipAlreadyExistsException` | native `INSERT … ON CONFLICT DO NOTHING` → 0 ⇒ исключение, 1 ⇒ `findById` |
| `findFriendsPage` / `…After` | native `UNION ALL` двух веток (индексы `idx_friendships_*_page`), курсор | тот же SQL, `@Query(nativeQuery)`, проекция `FriendView` |

### SubscriptionRepository (subscriptions)
| Метод | JPA |
|---|---|
| `createSubscription` | native `INSERT … ON CONFLICT DO NOTHING` → 0 ⇒ `SubscriptionAlreadyExistsException`, 1 ⇒ `findById` |
| `createSubscriptionIfAbsent` | тот же insert → 0 ⇒ пусто, 1 ⇒ `findById` |
| `existsSubscription` | `existsByFollowerIdAndFollowedId` |
| `findSubscription` | `findByFollowerIdAndFollowedId` |
| `deleteSubscription` | `@Modifying` JPQL `delete … where followerId and followedId` → 1 ⇒ true |
| `findByFollower` | `findByFollowerIdOrderByCreatedAtDescIdDesc` (как в уроке, без `Pageable`) |
| `findByFollowed` | `findByFollowedIdOrderByCreatedAtDescIdDesc` |

Все запросы ограничены `id` / `follower_id` / `followed_id` / `first_user_id` / `second_user_id`.

## Почему не `Slice` + `Pageable` для друзей
- OFFSET читает и отбрасывает все строки предыдущих страниц; при вставке новой дружбы между запросами страниц строки
  сдвигаются (дубли/пропуски). Курсор `(created_at, id) < (?, ?)` — нет.
- JPQL `first = :u or second = :u` + `order by created_at desc, id desc` не ложится на индексы
  `(first_user_id, created_at DESC, id DESC)` / `(second_user_id, …)` — PostgreSQL сортирует. `UNION ALL` двух веток с
  `LIMIT` в каждой читает ровно нужные строки каждого индекса.

## Кандидат в отдельную задачу
- Страницы подписок и подписчиков (`GET` подписок/подписчиков): порт, сценарии, контроллер, контракт `docs/api`,
  индексы `(follower_id, created_at DESC, id DESC)` / `(followed_id, …)` — сейчас индексы только по `follower_id`/`followed_id`.
