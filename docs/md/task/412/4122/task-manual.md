# 4122 — JPA-сущности и репозитории. План

Режим Ментора: ДА. Эталон — `decisions.md`; при расхождении плана с решением прав `decisions.md`.
Пункты выполняет Claude по команде пользователя; пользователь проверяет (Р-2).

Суть: таблица `friendship_offer` читается и пишется через Spring Data JPA вместо `JdbcTemplate`.
Порт `FriendshipOfferRepository` и сценарии не меняются (Р2-2, Р2-3) — меняется только адаптер.
Остальные таблицы — на JDBC до 4124–4126 (Р2-1). Схема БД не меняется (Р1-5).

| Итого | Новые | Изменить | Удалить |
|---|---|---|---|
| файлов | 3 (сущность, JPA-репозиторий, тест репозитория) | 7 | 1 (`FriendshipOfferRow`) |

Не меняются: миграции, порт-модели (`application/port/model/*`), сценарии (кроме Javadoc `CreateFriendshipOffer`),
`FriendshipsPersistenceAdapter`, `PostgresErrors`, `FriendshipOfferPersistenceAdapterTest`.

## Где будут файлы

```
pom.xml                                                         (изменить) data-jpa вместо jdbc, data-jpa-test
src/main/resources/application.yml                              (изменить) spring.jpa.*

src/main/java/ru/job4j/socialmediaapi/friendships/internal/
├── application/
│   ├── port/FriendshipOfferRepository.java                     (изменить) Javadoc create: без savepoint
│   └── usecase/CreateFriendshipOffer.java                      (изменить) Javadoc: без savepoint
└── infrastructure/persistence/
    ├── jpa/
    │   ├── FriendshipOfferEntity.java                          (новый) @Entity → friendship_offer
    │   └── FriendshipOfferJpaRepository.java                   (новый) Spring Data, Repository<…, UUID>
    ├── adapter/FriendshipOfferPersistenceAdapter.java          (изменить) JdbcTemplate → FriendshipOfferJpaRepository
    ├── mapper/FriendshipOfferPersistenceMapper.java            (изменить) Entity → модели порта
    └── row/FriendshipOfferRow.java                             (удалить)

src/test/java/ru/job4j/socialmediaapi/friendships/internal/infrastructure/persistence/
├── jpa/FriendshipOfferJpaRepositoryTest.java                   (новый) @DataJpaTest, 2 сценария урока
└── mapper/FriendshipOfferPersistenceMapperTest.java            (изменить) Entity вместо Row

docs/md/task/412/4122/manual-test-curl.md                       (новый) ручная проверка API заявок
```

## Файлы

| № | Файл | Слой | Действие | Решение |
|---|---|---|---|---|
| 1 | `pom.xml` | сборка | изменить | Р2-9 |
| 2 | `application.yml` | конфигурация | изменить | Р2-9 |
| 3 | `FriendshipOfferEntity` | infrastructure / persistence | новый | Р2-3, Р2-4, Р2-6 |
| 4 | `FriendshipOfferJpaRepository` | infrastructure / persistence | новый | Р2-5, Р2-7, Р2-8 |
| 5 | `FriendshipOfferJpaRepositoryTest` | тест | новый | Р2-10 |
| 6 | `FriendshipOfferPersistenceMapper` + тест | infrastructure / persistence | изменить | Р2-3 |
| 7 | `FriendshipOfferPersistenceAdapter`, удалить `FriendshipOfferRow` | infrastructure / persistence | изменить | Р2-2, Р2-7, Р2-8 |
| 8 | Javadoc порта и `CreateFriendshipOffer` | application | изменить | Р2-7 |
| 9 | прогон тестов | — | `./mvnw test` | Р2-10 |
| 10 | `manual-test-curl.md` | документация | новый | — |

## 1. pom.xml
Зачем: подключить Spring Data JPA / Hibernate и срез тестов `@DataJpaTest`.
Что сделать: `spring-boot-starter-jdbc` → `spring-boot-starter-data-jpa` (`JdbcTemplate` для JDBC-адаптеров остаётся — входит транзитивно);
добавить `spring-boot-starter-data-jpa-test` со scope `test`. Версии — от Spring Boot. Lombok уже подключён.

## 2. application.yml
Зачем: схему создаёт только Liquibase; Hibernate её сверяет и не держит сессию до конца HTTP-запроса.
```yaml
spring:
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
```

## 3. FriendshipOfferEntity
Зачем: отображение таблицы `friendship_offer` (Р-1: имя таблицы не меняем).
Что сделать: как в уроке — `@Entity @Table(name = "friendship_offer")`, `@Getter`, `@NoArgsConstructor(PROTECTED)`,
`@AllArgsConstructor(PACKAGE)`; поля `id` (`@Id`, без `@GeneratedValue` — Р2-6), `fromUserId`, `toUserId`,
`status` (`@Enumerated(STRING)`, `length = 16`, доменный `FriendshipOfferStatus` — Р2-4), `createdAt`, `updatedAt` (`Instant`);
`equals` по ненулевому id через геттер, `hashCode` — хеш класса. Сеттеров нет: запись — запросами репозитория.

## 4. FriendshipOfferJpaRepository
Зачем: запросы к `friendship_offer`; без `findAll`/`deleteAll` (Р2-5).
`interface FriendshipOfferJpaRepository extends Repository<FriendshipOfferEntity, UUID>`:

| Метод | Как | Для метода порта |
|---|---|---|
| `int create(id, fromUserId, toUserId, status, now)` | `@Modifying` native `INSERT … ON CONFLICT DO NOTHING` | `createFriendshipOffer` |
| `Optional<…> findById(UUID)` | производный | `findById`, чтение после вставки/обновления |
| `Optional<…> findByIdAndToUserId(UUID, UUID)` | производный | `findByIdAndRecipient` |
| `Optional<…> findByPairForUpdate(userA, userB)` | JPQL `(from=a and to=b) or (from=b and to=a)` + `@Lock(PESSIMISTIC_WRITE)` | `findByPair` |
| `int updateStatusIfPending(id, toUserId, status, updatedAt)` | `@Modifying(clearAutomatically = true)` JPQL `update … where status = PENDING` | `updateStatusIfPending` |
| `List<…> findIncomingFirstPage / findIncomingPageAfter / findOutgoingFirstPage / findOutgoingPageAfter` | native, SQL как сейчас в адаптере, `LIMIT :limit` | `findPage`, `findPageAfter` |

Все запросы ограничены `id` / `to_user_id` / `from_user_id` (правило проекта). Javadoc — на русском.

## 5. FriendshipOfferJpaRepositoryTest
Зачем: проверить отображение сущности и нативную вставку на PostgreSQL (задание урока, п. 3).
Что сделать: пакет `…persistence.jpa` (доступ к package-private конструктору), `@DataJpaTest`,
`@AutoConfigureTestDatabase(replace = NONE)`, `@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)`,
`@Import(TestcontainersConfiguration.class)`. Два сценария урока с `@DisplayName`:
- вставка → `create` вернул 1, `findById` равен ожидаемой сущности (`usingRecursiveComparison()`);
- повтор для той же пары → 0, хранится первая заявка, второй id не найден.

## 6. FriendshipOfferPersistenceMapper (+ тест)
Зачем: адаптер получает из JPA сущности, а порт отдаёт свои модели.
Что сделать: `toResponse`, `toFindResponse`, `toUpdateResponse`, `toCursor` — из `FriendshipOfferEntity`; `toRow` и `toStatus` убрать
(статус уже enum). Тест маппера: `toRow`/«неизвестный статус» — удалить (неприменимо), оставить по одному тесту на метод с сущностью.

## 7. FriendshipOfferPersistenceAdapter
Зачем: реализация порта через `FriendshipOfferJpaRepository` (Р2-2).
Что сделать:
- `createFriendshipOffer`: `create(...)` → 0 ⇒ `FriendshipOfferAlreadyExistsException`; 1 ⇒ `findById` → `toResponse`.
  Убрать `@Transactional(NESTED)`, `try/catch` и `PostgresErrors` (Р2-7). Транзакцию по-прежнему задаёт сценарий.
- `updateStatusIfPending`: `updateStatusIfPending(...)` → 0 ⇒ пусто; 1 ⇒ `findById`.
- `findPage`/`findPageAfter`: `checkLimit`, выбор запроса по `direction`, `limit + 1` строк, курсор — как сейчас.
- Удалить SQL-константы, `ROW_MAPPER`, `JdbcTemplate`, файл `FriendshipOfferRow`.
Проверка: `FriendshipOfferPersistenceAdapterTest` (21 тест) проходит без изменений.

## 8. Javadoc порта и CreateFriendshipOffer
Зачем: savepoint больше нет — дубль не прерывает транзакцию, потому что вставка его пропускает (`ON CONFLICT DO NOTHING`).
Что сделать: поправить абзацы про savepoint в `FriendshipOfferRepository#createFriendshipOffer` и в классе `CreateFriendshipOffer`. Код сценария не меняется.

## 9. Прогон тестов
`./mvnw test` — все тесты, включая E2E и `FriendshipOfferPersistenceAdapterTest`. `ApplicationModulesTest` входит в общий прогон.

## 10. manual-test-curl.md
Скрипт curl (OK/FAIL) по заявкам на локально запущенном приложении: создание → 201; повтор и встречная заявка → ошибка
«уже существует»; принятие получателем; входящие/исходящие страницы. Проверяет, что HTTP-поведение после перехода на JPA прежнее.

## После выполнения
- Коммит для ментора делает пользователь.
