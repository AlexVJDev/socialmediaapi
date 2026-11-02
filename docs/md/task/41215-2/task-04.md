# Задание 04. Тесты ленты: `FeedPersistenceAdapterTest` и `FriendshipFeedTest`

**Раздел README:** 2.8.
**Зависит от:** заданий 01–03.

## Цель

Вернуть проверки бизнес-процесса ленты на новом контракте и добавить проверки курсорных страниц.

## Контекст

- Прежние тесты — в коммите `38036d9`:
  - `src/test/java/ru/job4j/socialmediaapi/posts/internal/infrastructure/persistence/adapter/FeedPersistenceAdapterTest.java` (11 тестов);
  - `src/test/java/ru/job4j/socialmediaapi/FriendshipFeedTest.java` (2 теста через HTTP).
- Правила тестов проекта: `@SpringBootTest` + `@Import(TestcontainersConfiguration.class)`, Docker должен работать; данные — через порты (`PostRepository`) и API модуля (`Subscriptions`), `JdbcTemplate` — только чтобы прочитать строки или сломать ограничение намеренно; в каждом тесте свои `UUID.randomUUID()`; `@DisplayName` на русском.
- Вспомогательный метод в тестах: `feedIds(readerId)` — `findFeed(readerId, FeedRepository.MAX_LIMIT).posts()` → список `id`.

## Что сделать

### `FeedPersistenceAdapterTest`

1. **Перенести без изменения смысла** (меняется только вызов `findFeed`):
   - до подписки пост автора не в ленте;
   - после подписки — в ленте;
   - после отписки исчезает, сама публикация остаётся;
   - `DELETED` исключается, остальные посты автора остаются;
   - взаимные подписки: каждый видит посты другого, но не свои;
   - посты постороннего автора и свои — не в ленте;
   - несколько авторов — от новых к старым, все поля переносятся;
   - одинаковый `created_at` — по `id DESC`.
2. **Заменить** тест страниц `limit/offset`:
   - 5 постов, страницы по 2: `findFeed` → 2 поста и `next`; `findFeedAfter(next)` → 2 и `next`; затем 1 и пустой `next`. Вместе — все 5 в порядке ленты, без повторов.
3. **Новые сценарии:**
   - ровно `limit` постов в ленте → одна страница, `next` пуст (проверка `limit + 1`);
   - пустая лента (нет подписок) → `posts` пуст, `next` пуст;
   - одинаковый `created_at` у постов **разных** авторов на границе страниц (страница по 1–2 поста) → обход курсором даёт каждую запись ровно один раз;
   - у автора A постов больше, чем `limit`, у автора B меньше, посты перемешаны по времени → страницы совпадают с ожидаемым глобальным порядком (проверка `LIMIT` внутри `LATERAL`);
   - пост, опубликованный после чтения первой страницы, не появляется на следующей (он новее курсора);
   - пост второй страницы удалён между запросами → нет пропуска: следующая страница начинается с поста, шедшего за удалённым;
   - отписка между страницами → посты этого автора дальше не приходят, остальные — без пропусков.
4. **Аргументы:**
   - `limit` = 0 и отрицательный → `IllegalArgumentException`;
   - `limit = MAX_LIMIT + 1` → `IllegalArgumentException`; `limit = MAX_LIMIT` — работает;
   - `findFeedAfter(..., null, ...)` → `NullPointerException`.

### `FriendshipFeedTest`

Восстановить из `38036d9` целиком; изменить только `feedIds`:
```java
private List<UUID> feedIds(UUID readerId) {
    return feedRepository.findFeed(readerId, FeedRepository.MAX_LIMIT).posts()
            .stream().map(FeedPostResponse::id).toList();
}
```

## Проверка

- `./mvnw test -Dtest='FeedPersistenceAdapterTest,FriendshipFeedTest,FeedPersistenceMapperTest'`.
- `./mvnw test` — весь набор зелёный.

## Критерии приёмки

- Все 8 сценариев бизнес-процесса из `38036d9` сохранены.
- Обходы курсором проверяют одновременно порядок, отсутствие дублей и отсутствие пропусков (сравнение с полным ожидаемым списком, `containsExactly`).
