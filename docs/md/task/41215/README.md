# 41215. Публикации и пользовательская лента

Источник: `descr.pdf` (job4j, урок 4.1.2.1, задание #505784). Пункты 1–8 раздела «Задание» разбиты на шаги `task-01..08.md`; **все шаги выполнены**, `./mvnw verify` зелёный (99 тестов). Пункт 9 (коммит и ссылка на него) пользователь выполняет сам.

## Что сделано

- Таблица `posts`: публикации пользователей со статусами `PUBLISHED` / `DELETED` (логическое удаление) и индекс `(author_id, created_at DESC, id DESC)` — отдельными миграциями с rollback.
- `PostRepository` — создание, чтение, изменение и логическое удаление одной публикации. Изменять и удалять может только автор: `actorId` входит в контракт порта и в `WHERE` того же `UPDATE`.
- `FeedRepository` — лента читателя без отдельной таблицы: один SQL-запрос `subscriptions JOIN posts`, только `PUBLISHED`, сортировка `created_at DESC, id DESC`, страница через `LIMIT ? OFFSET ?`. Ограничения offset pagination описаны в `../../../process/Feed.md`.
- `SubscriptionRepository.deleteSubscription` — отписка, без неё нельзя проверить исчезновение поста из ленты.
- Интеграционные тесты на PostgreSQL Testcontainers, в том числе связь процессов: подписка → пост появился в ленте, отписка → исчез, `DELETED` → не виден.

## Имена: задание → проект

Имена из задания приведены к соглашениям проекта (`../../CLAUDE2.md`, раздел Architecture): строки таблиц — `*Row`, JDBC-адаптеры — `*RepositoryJdbc`, их тесты — `*RepositoryJdbcTest`.

| В задании | В проекте |
| --- | --- |
| `PostEntity` | `adapter.out.persistence.row.PostRow` |
| `PostRepository` (интерфейс) | `application.port.out.PostRepository` |
| реализация `PostRepository` через `JdbcTemplate` | `adapter.out.persistence.jdbc.PostRepositoryJdbc` |
| `FeedRepository` | `application.port.out.FeedRepository` + `adapter.out.persistence.jdbc.FeedRepositoryJdbc` |
| `JdbcPostRepositoryTest` | `PostRepositoryJdbcTest` |
| `JdbcFeedRepositoryTest` | `FeedRepositoryJdbcTest` |

## Шаги

| Файл | Пункты задания | Что сделано | Статус |
| --- | --- | --- | --- |
| [task-01.md](task-01.md) | 1 | Миграция `007-create-posts.sql`: таблица `posts`, `CHECK` `chk_posts_status`, `--rollback`; закомментированные примеры `INSERT` с допустимыми и недопустимыми статусами | ✅ |
| [task-02.md](task-02.md) | 2 | Миграция `008-create-posts-indexes.sql`: индекс `idx_posts_author_created` и rollback | ✅ |
| [task-03.md](task-03.md) | 3 | `PostStatus`, порт `PostRepository`, `PostRow`, `PostPersistenceMapper` + тест, `PostRepositoryJdbc` | ✅ |
| [task-04.md](task-04.md) | 4 | SQL создания, чтения, изменения и логического удаления в `PostRepositoryJdbc` | ✅ |
| [task-05.md](task-05.md) | 7 | `PostRepositoryJdbcTest` — 13 тестов | ✅ |
| [task-06.md](task-06.md) | 5, 6 | `FeedRepository`, `FeedRepositoryJdbc`, `FeedPersistenceMapper` + тест, `../../../process/Feed.md` | ✅ |
| [task-07.md](task-07.md) | — (нужно для 8) | `SubscriptionRepository.deleteSubscription` + 4 теста | ✅ |
| [task-08.md](task-08.md) | 7, 8 | `FeedRepositoryJdbcTest` — 11 тестов | ✅ |

## Отличия от первоначального плана

- **Один `PostResponse`** вместо `CreatePostResponse` / `FindPostResponse` / `UpdatePostResponse`: поля у них совпадают, причина записана в Javadoc порта.
- **`RowMapper` в каждом адаптере** (`private static final ROW_MAPPER`, как в `FriendshipsRepositoryJdbc`) вместо общего package-private `PostRowMapper`: `PostRepositoryJdbc` и `FeedRepositoryJdbc` маппят одни и те же колонки каждый сам.
- **У ленты свой `FeedPostResponse`** и свой `FeedPersistenceMapper` (только Row → Response), а не `PostRepository.PostResponse`: порты не зависят друг от друга.
- **`existsSubscription` оставлен**: на нём держатся проверки в тестах. В Javadoc — предупреждение, что вызывать его как проверку перед записью нельзя (check-then-act).

## Принятые решения

- **Без check-then-act.** Права автора и статус публикации проверяются в `WHERE` того же `UPDATE` (`id = ? AND author_id = ? AND status = 'PUBLISHED'`), отписка — один `DELETE`. Адаптер не различает «нет публикации», «не автор» и «уже удалена»: `Optional.empty()` / `false`.
- **Чтение `findPublishedById`** возвращает только `PUBLISHED`: удалённая публикация не видна и автору.
- **`limit <= 0` / `offset < 0`** в ленте — `IllegalArgumentException` до обращения к БД.
- **Данные в тестах ленты готовятся через порты** (`PostRepository`, `SubscriptionRepository`), а не через `JdbcTemplate`: так тест проверяет связь с бизнес-процессом, а не только SQL.
- **`title VARCHAR(255)`** — длина в задании не указана.

## Как проверить

```shell
./mvnw verify                                                      # все тесты + JaCoCo + Checkstyle (нужен Docker)
./mvnw test -Dtest='PostRepositoryJdbcTest,FeedRepositoryJdbcTest'  # только тесты задания
```

## Файлы

- Миграции: `../../../../src/main/resources/db/changes/007-create-posts.sql`, `008-create-posts-indexes.sql`, `db.changelog-master.yaml`.
- Код (`../../../../src/main/java/ru/job4j/socialmediaapi`): `domain/post/PostStatus`, `application/port/out/{PostRepository,FeedRepository}`, `adapter/out/persistence/row/PostRow`, `adapter/out/persistence/mapper/{PostPersistenceMapper,FeedPersistenceMapper}`, `adapter/out/persistence/jdbc/{PostRepositoryJdbc,FeedRepositoryJdbc}`; изменены `SubscriptionRepository`, `SubscriptionRepositoryJdbc`.
- Тесты (`src/test/java/.../adapter/out/persistence/`): `jdbc/{PostRepositoryJdbcTest,FeedRepositoryJdbcTest}`, `mapper/{PostPersistenceMapperTest,FeedPersistenceMapperTest}`; дополнен `jdbc/SubscriptionRepositoryJdbcTest`.
- Документация: `../../../process/Feed.md`, `../../CLAUDE2.md`.

## Вне рамок этого задания

- REST API публикаций и ленты (следующий блок курса «REST API как внешний адаптер») и их контракт в `../../../api`.
- Сервисы (`application`) публикаций: пока только порты и адаптеры. Время и id передаёт вызывающий (в тестах — фиксированные значения, в будущем сервисе — `Clock` и `UUID.randomUUID()`).
- Сценарий «после принятия предложения дружбы оба видят посты друг друга»: accept ещё не реализован, в `FeedRepositoryJdbcTest` он смоделирован двумя встречными подписками. Полноценный сквозной тест — после реализации accept.
- Cursor pagination — отдельное развитие процесса (вариант описан в `../../../process/Feed.md`).
