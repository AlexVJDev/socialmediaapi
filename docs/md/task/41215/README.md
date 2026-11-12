# 41215. Публикации и пользовательская лента

Источник: `descr.pdf` (job4j, урок 4.1.2.1, задание #505784). Пункты 1–8 раздела «Задание» разбиты на шаги `task-01..08.md`; **все шаги выполнены**, тесты задания зелёные. Пункт 9 (коммит и ссылка на него) пользователь выполняет сам.

Код задания живёт в модуле `posts` модульного монолита (Spring Modulith, правила — `../../../architecture.md`); отписка — в модуле `subscriptions`. Файлы `task-01..08.md` приведены к текущим именам и пакетам.

## Что сделано

- Таблица `posts`: публикации пользователей со статусами `PUBLISHED` / `DELETED` (логическое удаление) и индекс `(author_id, created_at DESC, id DESC)` — отдельными миграциями с rollback.
- `PostRepository` — создание, чтение, изменение и логическое удаление одной публикации. Изменять и удалять может только автор: `actorId` входит в контракт порта и в `WHERE` того же `UPDATE`.
- `FeedRepository` — лента читателя без отдельной таблицы: один SQL-запрос `subscriptions JOIN posts`, только `PUBLISHED`, сортировка `created_at DESC, id DESC`, страница через `LIMIT ? OFFSET ?`. Ограничения offset pagination описаны в `../../../process/Feed.md`.
- Отписка: `SubscriptionRepository.deleteSubscription` и метод API модуля `Subscriptions.unsubscribe` — без неё нельзя проверить исчезновение поста из ленты.
- Интеграционные тесты на PostgreSQL Testcontainers, в том числе связь процессов: подписка → пост появился в ленте, отписка → исчез, `DELETED` → не виден, принятая дружба → друзья видят посты друг друга.

## Имена: задание → проект

Имена из задания приведены к соглашениям проекта (`../../CLAUDE2_backup.md`, раздел Architecture): строки таблиц — `*Row`, JDBC-адаптеры — `*PersistenceAdapter`, их тесты — `*PersistenceAdapterTest`. Пакеты ниже — относительно `ru.job4j.socialmediaapi.posts.internal`.

| В задании | В проекте |
| --- | --- |
| `PostEntity` | `infrastructure.persistence.row.PostRow` |
| `PostRepository` (интерфейс) | `application.port.PostRepository` |
| реализация `PostRepository` через `JdbcTemplate` | `infrastructure.persistence.adapter.PostPersistenceAdapter` |
| `FeedRepository` | `application.port.FeedRepository` + `infrastructure.persistence.adapter.FeedPersistenceAdapter` |
| `JdbcPostRepositoryTest` | `PostPersistenceAdapterTest` |
| `JdbcFeedRepositoryTest` | `FeedPersistenceAdapterTest` |

## Шаги

| Файл | Пункты задания | Что сделано (текущие имена) | Статус |
| --- | --- | --- | --- |
| [task-01.md](task-01.md) | 1 | Миграция `007-create-posts.sql`: таблица `posts`, `CHECK` `chk_posts_status`, `--rollback`; закомментированные примеры `INSERT` с допустимыми и недопустимыми статусами | ✅ |
| [task-02.md](task-02.md) | 2 | Миграция `008-create-posts-indexes.sql`: индекс `idx_posts_author_created` и rollback | ✅ |
| [task-03.md](task-03.md) | 3 | `PostStatus`, порт `PostRepository` и его модели, `PostRow`, `PostPersistenceMapper` + тест, `PostPersistenceAdapter` | ✅ |
| [task-04.md](task-04.md) | 4 | SQL создания, чтения, изменения и логического удаления в `PostPersistenceAdapter` | ✅ |
| [task-05.md](task-05.md) | 7 | `PostPersistenceAdapterTest` — 13 тестов | ✅ |
| [task-06.md](task-06.md) | 5, 6 | `FeedRepository`, `FeedPersistenceAdapter`, `FeedPersistenceMapper` + тест, `../../../process/Feed.md` | ✅ |
| [task-07.md](task-07.md) | — (нужно для 8) | `SubscriptionRepository.deleteSubscription` + 4 теста в `SubscriptionPersistenceAdapterTest`; позже — `Subscriptions.unsubscribe` | ✅ |
| [task-08.md](task-08.md) | 7, 8 | `FeedPersistenceAdapterTest` — 11 тестов | ✅ |

## Отличия от первоначального плана

- **Один `PostResponse`** вместо `CreatePostResponse` / `FindPostResponse` / `UpdatePostResponse`: поля у них совпадают, причина записана в Javadoc порта.
- **`RowMapper` в каждом адаптере** (`private static final ROW_MAPPER`) вместо общего package-private `PostRowMapper`: `PostPersistenceAdapter` и `FeedPersistenceAdapter` маппят одни и те же колонки каждый сам.
- **У ленты свой `FeedPostResponse`** и свой `FeedPersistenceMapper` (только Row → Response), а не `PostResponse`: порты не зависят друг от друга.
- **`existsSubscription` оставлен**: на нём держатся проверки в тестах. В Javadoc — предупреждение, что вызывать его как проверку перед записью нельзя (check-then-act).
- **Переход на модульный монолит** (после выполнения шагов): код перенесён в модуль `posts` (`allowedDependencies = {}`), отписка — в модуль `subscriptions`. SQL ленты читает таблицу `subscriptions` чужого модуля — задокументированное исключение в `../../../architecture.md`.

## Принятые решения

- **Без check-then-act.** Права автора и статус публикации проверяются в `WHERE` того же `UPDATE` (`id = ? AND author_id = ? AND status = 'PUBLISHED'`), отписка — один `DELETE`. Адаптер не различает «нет публикации», «не автор» и «уже удалена»: `Optional.empty()` / `false`.
- **Чтение `findPublishedById`** возвращает только `PUBLISHED`: удалённая публикация не видна и автору.
- **`limit <= 0` / `offset < 0`** в ленте — `IllegalArgumentException` до обращения к БД.
- **Данные в тестах ленты готовятся через порты и API модулей** (`PostRepository`, `Subscriptions`), а не через `JdbcTemplate`: так тест проверяет связь с бизнес-процессом, а не только SQL.
- **`title VARCHAR(255)`** — длина в задании не указана.

## Как проверить

```shell
./mvnw verify                                                                 # все тесты + JaCoCo + Checkstyle (нужен Docker)
./mvnw test -Dtest='PostPersistenceAdapterTest,FeedPersistenceAdapterTest'    # только тесты задания
./mvnw test -Dtest=FriendshipFeedTest                                         # лента после accept/reject через HTTP
./mvnw test -Dtest=ApplicationModulesTest                                     # границы модулей (без Docker)
```

## Файлы

- Миграции (`../../../../src/main/resources/db`): `changes/007-create-posts.sql`, `changes/008-create-posts-indexes.sql`, `db.changelog-master.yaml`.
- Код модуля `posts` (`../../../../src/main/java/ru/job4j/socialmediaapi/posts`):
  - `package-info.java` — `@ApplicationModule`;
  - `internal/domain/model/PostStatus`;
  - `internal/application/port/{PostRepository,FeedRepository}`;
  - `internal/application/port/model/{CreatePostRequest,UpdatePostRequest,DeletePostRequest,PostResponse,FeedPostResponse}`;
  - `internal/infrastructure/persistence/row/PostRow`;
  - `internal/infrastructure/persistence/mapper/{PostPersistenceMapper,FeedPersistenceMapper}`;
  - `internal/infrastructure/persistence/adapter/{PostPersistenceAdapter,FeedPersistenceAdapter}`.
- Отписка в модуле `subscriptions` (`.../socialmediaapi/subscriptions`): `api/Subscriptions.unsubscribe`, `internal/application/service/SubscriptionsService`, `internal/application/port/SubscriptionRepository.deleteSubscription`, `internal/infrastructure/persistence/adapter/SubscriptionPersistenceAdapter`.
- Тесты (`../../../../src/test/java/ru/job4j/socialmediaapi`):
  - `posts/internal/infrastructure/persistence/adapter/{PostPersistenceAdapterTest,FeedPersistenceAdapterTest}`;
  - `posts/internal/infrastructure/persistence/mapper/{PostPersistenceMapperTest,FeedPersistenceMapperTest}`;
  - `subscriptions/internal/infrastructure/persistence/adapter/SubscriptionPersistenceAdapterTest` (тесты `deleteSubscription`);
  - `FriendshipFeedTest` — сквозная проверка ленты после accept/reject.
- Документация: `../../../process/Feed.md`, `../../CLAUDE2_backup.md`, `../../../architecture.md`.

## Вне рамок этого задания

- REST API публикаций и ленты (следующий блок курса «REST API как внешний адаптер») и их контракт в `../../../api`.
- Сценарии (`application.usecase`) и API модуля `posts`: пока только порты и адаптеры. Время и id передаёт вызывающий (в тестах — фиксированные значения, в будущем сценарии — `Clock` и `UUID.randomUUID()`).
- Сценарий «после принятия предложения дружбы оба видят посты друг друга»: в `FeedPersistenceAdapterTest` смоделирован двумя встречными подписками; сквозной тест через HTTP появился позже, вместе с accept/reject, — `FriendshipFeedTest` (корневой пакет тестов).
- Cursor pagination — отдельное развитие процесса (вариант описан в `../../../process/Feed.md`).
