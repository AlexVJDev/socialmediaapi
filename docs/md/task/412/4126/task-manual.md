# 4126 — Хранение и чтение пользовательской ленты. План

Режим Ментора: ДА. Эталон — `decisions.md`; при расхождении плана с решением прав `decisions.md`.
Код (включая тесты) пишет Claude по команде пользователя; тесты запускает пользователь (Р2-12).

Суть: лента читается через Spring Data JPA тем же запросом (Р6-1, Р6-2). После этого в `src/main` не остаётся JDBC:
все таблицы — через JPA. Порт `FeedRepository`, сценарии, API и схема БД не меняются (Р6-3).

| Итого | Новые | Изменить | Удалить |
|---|---|---|---|
| файлов | 2 | 2 | 3 |

## Где будут файлы

```
src/main/java/ru/job4j/socialmediaapi/posts/internal/infrastructure/persistence/
├── jpa/FeedJpaRepository.java                                   (новый) Repository<PostEntity, UUID>, запрос ленты
├── adapter/FeedPersistenceAdapter.java                          (изменить) JdbcTemplate → FeedJpaRepository
├── mapper/FeedPersistenceMapper.java                            (удалить)
└── row/PostRow.java                                             (удалить; каталог row/ пустеет)

src/test/java/ru/job4j/socialmediaapi/
├── posts/internal/infrastructure/persistence/
│   ├── jpa/FeedJpaRepositoryTest.java                           (новый) сценарий урока
│   └── mapper/FeedPersistenceMapperTest.java                    (удалить)
└── architecture/ApplicationModulesTest.java                     (изменить) правило *Row → *Entity/*JpaRepository
```

## Файлы

| № | Файл | Слой | Действие | Решение |
|---|---|---|---|---|
| 1 | `FeedJpaRepository` | infrastructure / persistence | новый | Р6-1, Р6-2 |
| 2 | `FeedPersistenceAdapter`; удалить `FeedPersistenceMapper`, `PostRow` | infrastructure / persistence | изменить | Р6-1 |
| 3 | `ApplicationModulesTest` | тест / архитектура | изменить | Р6-6 |
| 4 | `FeedJpaRepositoryTest`; удалить `FeedPersistenceMapperTest` | тест | новый | Р6-4, Р6-5, Р6-7 |
| 5 | компиляция + checkstyle (Claude); `./mvnw test` (пользователь) | — | — | Р2-12 |

## 1. FeedJpaRepository
`interface FeedJpaRepository extends Repository<PostEntity, UUID>`, `@Transactional(readOnly = true)`:
- `List<PostEntity> findFeedFirstPage(followerId, perAuthorLimit, limit)` — native, SQL `FIND_FIRST_PAGE` адаптера;
- `List<PostEntity> findFeedPageAfter(followerId, afterCreatedAt, afterId, perAuthorLimit, limit)` — native, `FIND_PAGE_AFTER`.
Литерал `p.status = 'PUBLISHED'` сохраняется (частичный индекс 008). Javadoc — суть запроса из Javadoc адаптера.

## 2. FeedPersistenceAdapter
- Зависимости: `FeedJpaRepository`, `PostPersistenceMapper` (`toFeedResponse`, `toCursor`).
- `findFeed` / `findFeedAfter`: `checkLimit`, `fetch = limit + 1` (на автора и на страницу), курсор — как сейчас.
- Удалить `JdbcTemplate`, SQL, `RowMapper`, `FeedPersistenceMapper`, `PostRow`.
Проверка (пользователь): `FeedPersistenceAdapterTest`, `PostFeedScenarioTest`, `FriendshipFeedTest` без изменений.

## 3. ApplicationModulesTest
Правило `persistenceRowsBelongToPersistence` → `persistenceModelsBelongToJpaPackage`:
классы с именем на `Entity` или `JpaRepository` (вне `shared`) лежат в `..*.internal.infrastructure.persistence.jpa`;
`@DisplayName` и текст `as(...)` — по-русски, как у соседних правил. Остальные правила не меняются.

## 4. FeedJpaRepositoryTest (`@DataJpaTest` + `TestcontainersConfiguration`)
Один сценарий (Р6-5): читатель подписан на автора (`SubscriptionJpaRepository.create`); у автора два PUBLISHED с одинаковым
`created_at` и один удалённый (`PostJpaRepository.save` + `deletePublished`); у другого пользователя — PUBLISHED.
`findFeedFirstPage(reader, 11, 10)` → id двух постов автора в порядке `id DESC` (сравнение по строке UUID — порядок PostgreSQL).

## 5. Проверка
Claude: `./mvnw -o compile test-compile`, `./mvnw -o checkstyle:check`. Пользователь: `./mvnw test`.

## После выполнения
- Задача 412 закрыта по всем подзадачам; коммит для ментора делает пользователь.
- Напоминание: `ApplicationModulesTest` — входит в общий прогон.
