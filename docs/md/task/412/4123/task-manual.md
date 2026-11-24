# 4123 — Тестирование репозитория с Testcontainers. План

Режим Ментора: ДА. Эталон — `decisions.md`; при расхождении плана с решением прав `decisions.md`.
Код (включая тестовый) пишет Claude по команде пользователя; тесты запускает пользователь (Р2-12, Р3-4).

Суть: API и сервис не меняются (Р3-1). Все интеграционные тесты работают на одном PostgreSQL-контейнере (Р3-2),
а интеграционный тест контроллера проверяет заявку в БД через Spring Data JPA, как в уроке (Р3-3).

| Итого | Новые | Изменить |
|---|---|---|
| файлов | 0 | 3 (только тесты) |

Не меняются: `src/main/**`, `E2ETest`, остальные тестовые классы (продолжают `@Import(TestcontainersConfiguration.class)`).

## Где будут файлы

```
src/test/java/ru/job4j/socialmediaapi/
├── TestcontainersConfiguration.java                                   (изменить) singleton-контейнер
└── friendships/internal/infrastructure/
    ├── persistence/adapter/FriendshipOfferPersistenceAdapterTest.java (изменить) @Import вместо своего @Container
    └── web/FriendshipOfferControllerTest.java                         (изменить) чтение заявок через JPA
```

## Файлы

| № | Файл | Слой | Действие | Решение |
|---|---|---|---|---|
| 1 | `TestcontainersConfiguration` | тест / инфраструктура | изменить | Р3-2 |
| 2 | `FriendshipOfferPersistenceAdapterTest` | тест | изменить | Р3-2 |
| 3 | `FriendshipOfferControllerTest` | тест | изменить | Р3-3 |
| 4 | прогон тестов | — | пользователь | Р2-12 |

## 1. TestcontainersConfiguration — один контейнер на прогон
Зачем: сейчас каждый Spring-контекст (`@SpringBootTest`+MockMvc, `@E2ETest`, `@DataJpaTest`, тесты адаптеров)
поднимает свой PostgreSQL. Один контейнер — быстрее прогон, как «общий контейнер» урока.
Что сделать:
- статическое поле `POSTGRES = new PostgreSQLContainer("postgres:17")`, запуск в статическом блоке (`start()`);
- `@Bean(destroyMethod = "") @ServiceConnection PostgreSQLContainer postgresContainer()` возвращает `POSTGRES`:
  закрытие контекста не останавливает контейнер; его останавливает Testcontainers (Ryuk) в конце JVM;
- Javadoc: контейнер общий, тесты не чистят базу — у каждого свои `UUID.randomUUID()`; Liquibase применяет миграции
  один раз, в остальных контекстах — «Database is up to date».

## 2. FriendshipOfferPersistenceAdapterTest
Зачем: у класса свой `@Container` — ещё один контейнер (Р3-2).
Что сделать: убрать `@Testcontainers`, поле `POSTGRES` с `@Container @ServiceConnection` и их импорты;
добавить `@Import(TestcontainersConfiguration.class)`. Тесты не меняются.
Проверить при прогоне: тесты класса не рассчитывают на пустую таблицу (используют свои UUID).

## 3. FriendshipOfferControllerTest — проверка заявки через JPA
Зачем: урок проверяет результат HTTP-запроса в БД через Spring Data JPA и сравнивает заявку после повтора целиком.
Что сделать:
- внедрить `FriendshipOfferJpaRepository` (публичный интерфейс модуля friendships, тот же модуль);
- «Новая заявка: 201…»: заявку по id из ответа читать `findById`; статус `PENDING`; поля ответа
  (`id`, `fromUserId`, `toUserId`, `status`, `createdAt`) совпадают с сущностью;
- «Повтор заявки в статусе PENDING: 200…»: прочитать заявку до повтора и после; сравнить
  `usingRecursiveComparison()` — заявка не изменилась;
- `UPDATE friendship_offer …` (подготовка данных), подписки и дружбы — без изменений, через `JdbcTemplate`.

## 4. Прогон (пользователь)
`./mvnw test`. В логе — один запуск контейнера `postgres:17` на весь прогон.

## После выполнения
- Коммит для ментора делает пользователь.
