# 4125 — Публикации и пользовательская лента. План

Режим Ментора: ДА. Эталон — `decisions.md`; при расхождении плана с решением прав `decisions.md`.
Код (включая тесты) пишет Claude по команде пользователя; тесты запускает пользователь (Р2-12).

Суть: таблица `posts` для операций с публикациями читается и пишется через Spring Data JPA (Р5-1).
Порт `PostRepository`, сценарии, API, схема БД не меняются; лента — 4126. Миграция `posts` уже есть (007, 008).

| Итого | Новые | Изменить |
|---|---|---|
| файлов | 4 | 3 |

## Где будут файлы

```
src/main/java/ru/job4j/socialmediaapi/posts/internal/infrastructure/persistence/
├── jpa/
│   ├── PostEntity.java                                   (новый) @Entity → posts, Persistable<UUID>
│   └── PostJpaRepository.java                            (новый) Repository<PostEntity, UUID>
├── adapter/PostPersistenceAdapter.java                   (изменить) JdbcTemplate → JPA
├── mapper/PostPersistenceMapper.java                     (изменить) PostEntity → модели порта
├── adapter/FeedPersistenceAdapter.java                   (без изменений, 4126)
├── mapper/FeedPersistenceMapper.java                     (без изменений, 4126)
└── row/PostRow.java                                      (без изменений, 4126)

src/test/java/ru/job4j/socialmediaapi/posts/internal/infrastructure/persistence/
├── jpa/PostJpaRepositoryTest.java                        (новый) сценарий урока
├── jpa/PostEntityFixtures.java                           (новый) сущность для тестов других пакетов
└── mapper/PostPersistenceMapperTest.java                 (изменить) Entity вместо Row
```

## Файлы

| № | Файл | Слой | Действие | Решение |
|---|---|---|---|---|
| 1 | `PostEntity` | infrastructure / persistence | новый | Р5-2, Р5-3 |
| 2 | `PostJpaRepository` | infrastructure / persistence | новый | Р5-2…Р5-6 |
| 3 | `PostPersistenceMapper` + тест, `PostEntityFixtures` | infrastructure / persistence | изменить | Р5-7 |
| 4 | `PostPersistenceAdapter` | infrastructure / persistence | изменить | Р5-1, Р5-3, Р5-5 |
| 5 | `PostJpaRepositoryTest` | тест | новый | Р5-8 |
| 6 | компиляция + checkstyle (Claude); `./mvnw test` (пользователь) | — | — | Р2-12 |

## 1. PostEntity
- `@Entity @Table(name = "posts")`; поля `id`, `authorId`, `title` (`length = 255`), `content`, `status`
  (`@Enumerated(STRING)`, `length = 16`, доменный `PostStatus`), `createdAt`, `updatedAt`; Lombok, `equals`/`hashCode` — как в 4122.
- `implements Persistable<UUID>`: `@Transient boolean isNew`; `isNew()` возвращает его; `@PostPersist`/`@PostLoad` → `false`.
- Создание новой публикации — `public static PostEntity newPost(id, authorId, title, content, status, createdAt, updatedAt)`
  (адаптер в другом пакете; `isNew = true`). Конструктор со всеми полями — package-private (тесты пакета). Сеттеров нет (Р5-5).

## 2. PostJpaRepository (`@Transactional(readOnly = true)`)
| Метод | Как | Метод порта |
|---|---|---|
| `PostEntity save(PostEntity)` | Spring Data (`@Transactional`) | `createPost` |
| `Optional<PostEntity> findById(UUID)` | производный | чтение после обновления |
| `Optional<PostEntity> findByIdAndStatus(UUID, PostStatus)` | производный (урок) | `findPublishedById` |
| `int updatePublished(id, authorId, title, content, updatedAt)` | `@Transactional @Modifying(clearAutomatically = true)` JPQL | `updatePost` |
| `int deletePublished(id, authorId, deletedAt)` | то же, `status = DELETED` | `deletePost` |
| `List<PostEntity> findPublishedByAuthorFirstPage(authorId, limit)` | native, литерал `'PUBLISHED'` | `findPublishedByAuthor` |
| `List<PostEntity> findPublishedByAuthorPageAfter(authorId, afterCreatedAt, afterId, limit)` | native, курсор | `findPublishedByAuthorAfter` |

## 3. PostPersistenceMapper (+ тест)
`toResponse(PostEntity) → PostResponse`, `toFeedResponse(PostEntity) → FeedPostResponse`, `toCursor(PostEntity) → FeedCursor`;
`toRow` убрать. `PostEntityFixtures` (тестовый пакет `jpa`) — как `FriendshipOfferEntityFixtures`. Тест — по одному на метод.

## 4. PostPersistenceAdapter
- `createPost`: `save(PostEntity.newPost(...))` → `toResponse`.
- `findPublishedById`: `findByIdAndStatus(id, PUBLISHED)`.
- `updatePost`: `updatePublished(...)` → 0 ⇒ пусто; 1 ⇒ `findById`.
- `deletePost`: `deletePublished(...) == 1`.
- страницы: `checkLimit`, `limit + 1`, курсор — как сейчас, маппинг — `PostPersistenceMapper`.
- Удалить `JdbcTemplate`, SQL, `RowMapper`, зависимость от `FeedPersistenceMapper`.
Проверка (пользователь): `PostPersistenceAdapterTest`, `PostControllerTest`, `PostFeedScenarioTest` без изменений.

## 5. PostJpaRepositoryTest (`@DataJpaTest` + `TestcontainersConfiguration`)
Один сценарий урока: `save(newPost(…PUBLISHED…))` → `findByIdAndStatus(id, PUBLISHED)` равен сохранённой (`usingRecursiveComparison`,
без поля `isNew`) → `deletePublished(id, author, t)` = 1 → `findByIdAndStatus(id, PUBLISHED)` пуст, `findById(id)` — статус `DELETED`.

## 6. Проверка
Claude: `./mvnw -o compile test-compile`, `./mvnw -o checkstyle:check`. Пользователь: `./mvnw test`.

## После выполнения
- Коммит для ментора делает пользователь.
