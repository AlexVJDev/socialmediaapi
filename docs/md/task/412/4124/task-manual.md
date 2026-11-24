# 4124 — Запросы подписчиков и друзей. План

Режим Ментора: ДА. Эталон — `decisions.md`; при расхождении плана с решением прав `decisions.md`.
Код (включая тесты) пишет Claude по команде пользователя; тесты запускает пользователь (Р2-12).

Суть: таблицы `friendships` и `subscriptions` читаются и пишутся через Spring Data JPA вместо `JdbcTemplate` (Р4-1).
Порты, сценарии, API и схема БД не меняются. Образец — 4122 (`FriendshipOfferEntity`, `FriendshipOfferJpaRepository`).

| Итого | Новые | Изменить | Удалить |
|---|---|---|---|
| файлов | 9 | 6 | 5 |

## Где будут файлы

```
src/main/resources/application.yml                                        (изменить) лог SQL

src/main/java/ru/job4j/socialmediaapi/friendships/internal/infrastructure/persistence/
├── jpa/
│   ├── FriendshipEntity.java                                             (новый) @Entity → friendships
│   ├── FriendView.java                                                   (новый) проекция строки страницы друзей
│   └── FriendshipJpaRepository.java                                      (новый) Repository<FriendshipEntity, UUID>
├── adapter/FriendshipsPersistenceAdapter.java                            (изменить) JdbcTemplate → JPA
├── adapter/PostgresErrors.java                                           (удалить)
├── mapper/FriendshipPersistenceMapper.java                               (изменить) Entity/FriendView → модели порта
└── row/FriendshipsRow.java, row/FriendRow.java                           (удалить; каталог row/ пустеет)

src/main/java/ru/job4j/socialmediaapi/subscriptions/internal/infrastructure/persistence/
├── jpa/
│   ├── SubscriptionEntity.java                                           (новый) @Entity → subscriptions
│   └── SubscriptionJpaRepository.java                                    (новый) Repository<SubscriptionEntity, UUID>
├── adapter/SubscriptionPersistenceAdapter.java                           (изменить) JdbcTemplate → JPA
├── adapter/PostgresErrors.java                                           (удалить)
├── mapper/SubscriptionPersistenceMapper.java                             (изменить) Entity → модели порта
└── row/SubscriptionRow.java                                              (удалить)

src/test/java/ru/job4j/socialmediaapi/
├── friendships/internal/infrastructure/persistence/
│   ├── jpa/FriendshipJpaRepositoryTest.java                              (новый) друг с любой стороны пары
│   ├── jpa/FriendshipEntityFixtures.java                                 (новый) сущность для тестов других пакетов
│   └── mapper/FriendshipPersistenceMapperTest.java                       (изменить) Entity вместо Row
└── subscriptions/internal/infrastructure/persistence/
    ├── jpa/SubscriptionJpaRepositoryTest.java                            (новый) направление подписки
    └── jpa/SubscriptionEntityFixtures.java                               (новый) сущность для тестов (если понадобится)
```

## Файлы

| № | Файл | Слой | Действие | Решение |
|---|---|---|---|---|
| 1 | `application.yml` | конфигурация | лог SQL | Р4-8 |
| 2 | `FriendshipEntity`, `FriendView`, `FriendshipJpaRepository` | infrastructure / persistence | новые | Р4-2, Р4-3, Р4-4, Р4-6 |
| 3 | `FriendshipPersistenceMapper` + тест | infrastructure / persistence | изменить | Р4-7 |
| 4 | `FriendshipsPersistenceAdapter`; удалить `PostgresErrors`, `FriendshipsRow`, `FriendRow` | infrastructure / persistence | изменить | Р4-1, Р4-6, Р4-7 |
| 5 | `SubscriptionEntity`, `SubscriptionJpaRepository` | infrastructure / persistence | новые | Р4-2, Р4-5, Р4-6 |
| 6 | `SubscriptionPersistenceMapper` | infrastructure / persistence | изменить | Р4-7 |
| 7 | `SubscriptionPersistenceAdapter`; удалить `PostgresErrors`, `SubscriptionRow` | infrastructure / persistence | изменить | Р4-1, Р4-6, Р4-7 |
| 8 | `FriendshipJpaRepositoryTest`, `SubscriptionJpaRepositoryTest` | тест | новые | Р4-9 |
| 9 | компиляция + checkstyle (Claude); `./mvnw test` (пользователь) | — | — | Р2-12 |

## 1. application.yml
```yaml
logging:
  level:
    org.hibernate.SQL: ${SQL_LOG_LEVEL:info}   # debug — SQL каждого запроса в лог (локальный стенд)
```

## 2. friendships: сущность, проекция, репозиторий
- `FriendshipEntity` (`@Table(name = "friendships")`): `id`, `firstUserId`, `secondUserId`, `friendshipOfferId` (UUID, Р4-3),
  `status` (`@Enumerated(STRING)`, доменный `FriendshipStatus`), `createdAt`; Lombok и `equals`/`hashCode` — как `FriendshipOfferEntity`.
- `FriendView` — интерфейс-проекция нативного запроса: `getId()`, `getFriendId()`, `getCreatedAt()`.
- `FriendshipJpaRepository extends Repository<FriendshipEntity, UUID>`, `@Transactional(readOnly = true)`:

| Метод | Как |
|---|---|
| `int create(id, firstUserId, secondUserId, friendshipOfferId, status, createdAt)` | `@Transactional @Modifying` native `INSERT … ON CONFLICT DO NOTHING` |
| `Optional<FriendshipEntity> findById(UUID)` | производный |
| `List<FriendView> findFriendsFirstPage(userId, limit)` | native `UNION ALL` (SQL как сейчас), `LIMIT :limit` в ветках и в общем запросе |
| `List<FriendView> findFriendsPageAfter(userId, afterCreatedAt, afterId, limit)` | то же с `(created_at, id) < (:afterCreatedAt, :afterId)` |

## 3. FriendshipPersistenceMapper (+ тест)
`toResponse(FriendshipEntity)`, `toFindResponse(FriendView)` → `FindFriendResponse(id, userId = friendId)`,
`toCursor(FriendView)` → `FriendCursor`; `toRow` убрать. Тест — как `FriendshipOfferPersistenceMapperTest` (фикстура из пакета `jpa`).

## 4. FriendshipsPersistenceAdapter
- `createFriendship`: `create(...)` → 0 ⇒ `FriendshipAlreadyExistsException(first, second, null)`; 1 ⇒ `findById` → `toResponse`.
- `findFriendsPage` / `findFriendsPageAfter`: проверки и `limit + 1`, курсор — как сейчас, строки из `FriendView`.
- Удалить `JdbcTemplate`, SQL-константы, `RowMapper`-ы, `PostgresErrors`, `FriendshipsRow`, `FriendRow`.
Проверка (пользователь): `FriendshipsPersistenceAdapterTest`, `FriendshipE2ETest` без изменений.

## 5. subscriptions: сущность и репозиторий
- `SubscriptionEntity` (`@Table(name = "subscriptions")`): `id`, `followerId`, `followedId`, `createdAt`.
- `SubscriptionJpaRepository extends Repository<SubscriptionEntity, UUID>`, `@Transactional(readOnly = true)`:

| Метод | Как | Метод порта |
|---|---|---|
| `int create(id, followerId, followedId, createdAt)` | `@Transactional @Modifying` native `INSERT … ON CONFLICT DO NOTHING` | `createSubscription`, `createSubscriptionIfAbsent` |
| `Optional<…> findById(UUID)` | производный | чтение после вставки |
| `boolean existsByFollowerIdAndFollowedId` | производный | `existsSubscription` |
| `Optional<…> findByFollowerIdAndFollowedId` | производный | `findSubscription` |
| `int deleteSubscription(followerId, followedId)` | `@Transactional @Modifying` JPQL `delete` | `deleteSubscription` |
| `List<…> findByFollowerIdOrderByCreatedAtDescIdDesc` | производный (имя из урока) | `findByFollower` |
| `List<…> findByFollowedIdOrderByCreatedAtDescIdDesc` | производный (имя из урока) | `findByFollowed` |

## 6. SubscriptionPersistenceMapper
`toResponse(SubscriptionEntity)`, `toResponses(List<SubscriptionEntity>)`; `toRow` убрать.

## 7. SubscriptionPersistenceAdapter
- `createSubscription`: 0 ⇒ `SubscriptionAlreadyExistsException(follower, followed, null)`; 1 ⇒ `findById`.
- `createSubscriptionIfAbsent`: 0 ⇒ `Optional.empty()`; 1 ⇒ `findById`.
- остальные — прямые вызовы репозитория; `deleteSubscription` → `== 1`.
- Удалить `JdbcTemplate`, SQL, `RowMapper`, `PostgresErrors`, `SubscriptionRow`.
Проверка (пользователь): `SubscriptionPersistenceAdapterTest` без изменений.

## 8. Тесты репозиториев (`@DataJpaTest` + `TestcontainersConfiguration`, как `FriendshipOfferJpaRepositoryTest`)
- `FriendshipJpaRepositoryTest`: заявка ACCEPTED (через `FriendshipOfferJpaRepository.create` — FK) и дружба `(a, b)`, `a < b`;
  `findFriendsFirstPage(a)` → друг `b`, `findFriendsFirstPage(b)` → друг `a`.
- `SubscriptionJpaRepositoryTest`: подписка `A → B`; подписчики `B` — `[A]`, подписчики `A` — пусто; подписки `A` — `[B]`.

## 9. Проверка
Claude: `./mvnw -o compile test-compile`, `./mvnw -o checkstyle:check`. Пользователь: `./mvnw test`.

## После выполнения
- Коммит для ментора делает пользователь.
