# Инвентаризация перед рефакторингом в модульный монолит

Этап 0 из `docs/refactoring-kit/02-refactoring-guide.md`, снимок на 2026-10-16 (ветка `main`, коммит `6bf5e17`).
Целевая структура — `docs/refactoring-kit/01-target-structure.md`.

> Набор `docs/refactoring-kit/` удалён после завершения рефакторинга; ссылки на него ниже — исторические.
> Скрипт схемы пакетов перенесён в `../scripts`, действующие правила — в `docs/architecture.md`.

## 1. Стек и сборка

| Что | Значение |
|---|---|
| Java | 21 (`java.version` в `../../../pom.xml`; было 17, поднята 2026-10-16) |
| Spring Boot | 4.1.1 (`spring-boot-starter-parent`), `webmvc`, `validation`, `actuator`, `jdbc`, `liquibase` |
| Сборщик | Maven (`./mvnw`), Checkstyle (`../../../checkstyle.xml`, `JavadocType`/`JavadocMethod` валят сборку), JaCoCo |
| Хранение | PostgreSQL 17, `JdbcTemplate` + нативный SQL, **без JPA и Spring Data**; миграции — Liquibase (`db/changes/001`–`008`) |
| MapStruct / Lombok | 1.6.3 / версия из Boot, порядок процессоров: `lombok` → `lombok-mapstruct-binding` → `mapstruct-processor` → `spring-boot-configuration-processor` |
| Spring Modulith, ArchUnit | **нет**. План: `spring-modulith-bom` 2.1.1 (линия 2.1 собрана под Boot 4.1), `spring-modulith-starter-core`, `spring-modulith-starter-test` (test). ArchUnit приходит транзитивно из `spring-modulith-core`; `starter-jdbc` и таблица `event_publication` не нужны — событий нет |
| Kafka, Temporal, Feign, Security | нет |
| Тесты | `./mvnw test`; интеграционные — Testcontainers (`postgres:17`), **нужен Docker** |
| Полная проверка | `./mvnw verify` (тесты + JaCoCo + Checkstyle); CI — `../../../.github/workflows/maven.yml` |

## 2. Текущая схема пакетов

`python3 -I scripts/gen-schema.py src/main/java/ru/job4j/socialmediaapi --classes`
(базовый пакет `ru.job4j.socialmediaapi`, главный класс `SocialMediaApiApplication`):

```text
├── adapter/
│   ├── in/
│   │   └── web/  ‹class @RestController›
│   │       ├── FriendshipOfferController  ‹class @RestController›
│   │       ├── PingController  ‹class @RestController›
│   │       ├── dto/  ‹record›
│   │       │   ├── CreateFriendshipOfferWebRequest  ‹record›
│   │       │   ├── CreateFriendshipOfferWebResponse  ‹record›
│   │       │   └── ErrorResponse  ‹record›
│   │       ├── error/  ‹enum, class @RestControllerAdvice›
│   │       │   ├── ErrorCode  ‹enum›
│   │       │   └── GlobalExceptionHandler  ‹class @RestControllerAdvice›
│   │       └── mapper/  ‹interface @Mapper›
│   │           └── FriendshipOfferWebMapper  ‹interface @Mapper›
│   └── out/
│       └── persistence/
│           ├── jdbc/  ‹class @Repository, class›
│           │   ├── FeedRepositoryJdbc  ‹class @Repository — implements FeedRepository›
│           │   ├── FriendshipOfferRepositoryJdbc  ‹class @Repository — implements FriendshipOfferRepository›
│           │   ├── FriendshipsRepositoryJdbc  ‹class @Repository — implements FriendshipsRepository›
│           │   ├── PostRepositoryJdbc  ‹class @Repository — implements PostRepository›
│           │   ├── PostgresErrors  ‹class›
│           │   └── SubscriptionRepositoryJdbc  ‹class @Repository — implements SubscriptionRepository›
│           ├── mapper/  ‹interface @Mapper›
│           │   ├── FeedPersistenceMapper  ‹interface @Mapper›
│           │   ├── FriendshipOfferPersistenceMapper  ‹interface @Mapper›
│           │   ├── FriendshipPersistenceMapper  ‹interface @Mapper›
│           │   ├── PostPersistenceMapper  ‹interface @Mapper›
│           │   └── SubscriptionPersistenceMapper  ‹interface @Mapper›
│           └── row/  ‹record›
│               ├── FriendshipOfferRow  ‹record›
│               ├── FriendshipsRow  ‹record›
│               ├── PostRow  ‹record›
│               └── SubscriptionRow  ‹record›
├── application/  ‹class @Service›
│   ├── FriendshipOfferService  ‹class @Service — implements CreateFriendshipOfferUseCase›
│   ├── mapper/  ‹interface @Mapper›
│   │   └── FriendshipOfferApplicationMapper  ‹interface @Mapper›
│   └── port/
│       ├── in/  ‹interface›
│       │   └── CreateFriendshipOfferUseCase  ‹interface›
│       └── out/  ‹interface›
│           ├── FeedRepository  ‹interface›
│           ├── FriendshipOfferRepository  ‹interface›
│           ├── FriendshipsRepository  ‹interface›
│           ├── PostRepository  ‹interface›
│           └── SubscriptionRepository  ‹interface›
├── config/  ‹class @Configuration›
│   └── ClockConfiguration  ‹class @Configuration›
└── domain/
    ├── friendship/  ‹enum›
    │   ├── FriendshipStatus  ‹enum›
    │   └── exception/  ‹class (исключение)›
    │       └── FriendshipAlreadyExistsException  ‹class — extends RuntimeException›
    ├── friendshipoffer/  ‹record, enum›
    │   ├── FriendshipOffer  ‹record›
    │   ├── FriendshipOfferStatus  ‹enum›
    │   └── exception/  ‹class (исключение)›
    │       ├── FriendshipOfferAlreadyAcceptedException  ‹class — extends RuntimeException›
    │       ├── FriendshipOfferAlreadyExistsException  ‹class — extends RuntimeException›
    │       ├── FriendshipOfferAlreadyPendingException  ‹class — extends RuntimeException›
    │       ├── FriendshipOfferAlreadyRejectedException  ‹class — extends RuntimeException›
    │       ├── FriendshipOfferToSelfException  ‹class — extends RuntimeException›
    │       └── FriendshipRequestNotFoundException  ‹class — extends RuntimeException›
    ├── post/  ‹enum›
    │   └── PostStatus  ‹enum›
    └── subscription/
        └── exception/  ‹class (исключение)›
            ├── SubscriptionAlreadyExistsException  ‹class — extends RuntimeException›
            └── SubscriptionToSelfException  ‹class — extends RuntimeException›
```

## 3. Инвентаризация

### Контроллеры

| Класс | URL | Вызывает |
|---|---|---|
| `adapter/in/web/FriendshipOfferController` | `POST /api/offer-friendships/create` | `CreateFriendshipOfferUseCase` |
| `adapter/in/web/PingController` | `GET /api/ping` | — |
| `adapter/in/web/error/GlobalExceptionHandler` (`@RestControllerAdvice`) | все | — |

`GlobalExceptionHandler` смешивает две ответственности:
- **заявки в друзья:** `MethodArgumentNotValidException` / `HttpMessageNotReadableException` / `FriendshipOfferToSelfException` → `400 INVALID_FRIENDSHIP_OFFER_REQUEST`; `FriendshipOfferAlready{Pending,Accepted,Rejected}Exception` → `409`; `FriendshipOfferAlreadyExistsException` → WARN + `204`;
- **общее:** `DataIntegrityViolationException` → лог + `500 INTERNAL_ERROR`.

`ErrorCode` содержит четыре кода заявок и общий `INTERNAL_ERROR`.

### Таблицы и хранение (JDBC, сущностей `@Entity` нет)

| Таблица (changeset) | Row | Порт → адаптер | Mapper |
|---|---|---|---|
| `friendship_offer` (001, 002) | `FriendshipOfferRow` | `FriendshipOfferRepository` → `FriendshipOfferRepositoryJdbc` | `FriendshipOfferPersistenceMapper` |
| `friendships` (003, 004), FK на `friendship_offer` | `FriendshipsRow` | `FriendshipsRepository` → `FriendshipsRepositoryJdbc` | `FriendshipPersistenceMapper` |
| `subscriptions` (005, 006) | `SubscriptionRow` | `SubscriptionRepository` → `SubscriptionRepositoryJdbc` | `SubscriptionPersistenceMapper` |
| `posts` (007, 008) | `PostRow` | `PostRepository` → `PostRepositoryJdbc` | `PostPersistenceMapper` |
| `subscriptions JOIN posts` (чтение) | `PostRow` | `FeedRepository` → `FeedRepositoryJdbc` | `FeedPersistenceMapper` |

`PostgresErrors` (package-private, `jdbc`) — достаёт имя констрейнта из `DataIntegrityViolationException`; используется адаптерами `FriendshipOffer`, `Friendships`, `Subscription`, то есть тремя будущими модулями.

### Прочее

| Категория | Что есть |
|---|---|
| Внешние клиенты (Feign / RestTemplate / WebClient / RestClient) | нет |
| Kafka, `@Scheduled`, Temporal | нет |
| Spring-события (`ApplicationEventPublisher`, `@EventListener`) | нет |
| `@Configuration` | `config/ClockConfiguration` (бин `Clock`, UTC) |
| `@ConfigurationProperties` | нет |
| `@EntityScan`, `@EnableJpaRepositories`, `@ComponentScan` с пакетами | нет — всё сканируется от `SocialMediaApiApplication` |
| `application.yml`, `logging.level.<package>`, `META-INF/spring/*.imports` | имён пакетов нет |
| MapStruct `uses` / `imports` с полными именами, SpEL | нет |

### Тесты (`src/test/java/ru/job4j/socialmediaapi/…`)

| Тест | Будущий модуль |
|---|---|
| `SocialMediaApiApplicationTests`, `TestcontainersConfiguration` | корень (остаются на месте) |
| `adapter/in/web/FriendshipOfferControllerTest`, `adapter/in/web/mapper/FriendshipOfferWebMapperTest` | `friendships` |
| `adapter/in/web/error/GlobalExceptionHandlerTest` | делится: `friendships` (409/204) и `shared` (500) |
| `application/FriendshipOfferServiceTest`, `application/FriendshipOfferServiceConcurrencyTest` | `friendships` |
| `domain/friendshipoffer/FriendshipOfferTest` | `friendships` |
| `…/persistence/jdbc/FriendshipOfferRepositoryJdbcTest`, `FriendshipsRepositoryJdbcTest`, `…/mapper/FriendshipOfferPersistenceMapperTest`, `FriendshipPersistenceMapperTest` | `friendships` |
| `…/persistence/jdbc/SubscriptionRepositoryJdbcTest` | `subscriptions` |
| `…/persistence/jdbc/PostRepositoryJdbcTest`, `FeedRepositoryJdbcTest`, `…/mapper/PostPersistenceMapperTest`, `FeedPersistenceMapperTest` | `posts` |

`FeedRepositoryJdbcTest` создаёт подписки через `SubscriptionRepository`, `FriendshipOfferServiceTest` мокирует его — после переноса они будут обращаться к `subscriptions.api`.

## 4. Хранимые имена классов

Нет. Полные имена классов не попадают ни в БД (нет `event_publication`, JSONB с `@JsonTypeInfo`), ни в Kafka, ни в кэш, ни в Java-сериализацию, ни в Temporal. Классы можно переносить без алиасов и миграций данных.

## 5. Карта модулей

Согласовано: три бизнес-модуля, плюс технический `system` для `/api/ping`, плюс `shared`.
Имена пакетов — множественное число, как в шаблоне.

### `subscriptions` — подписки читателя на автора

- **Данные:** таблица `subscriptions`.
- **Классы:** `SubscriptionRepository`, `SubscriptionRepositoryJdbc`, `SubscriptionRow`, `SubscriptionPersistenceMapper`, `SubscriptionAlreadyExistsException`, `SubscriptionToSelfException`.
- **`allowedDependencies`:** `{}`.
- **`api`:** интерфейс `Subscriptions` (`@NamedInterface("api")`):
  `subscribeIfAbsent(followerId, followedId, createdAt)` — для `friendships` (сейчас `createSubscriptionIfAbsent`);
  `unsubscribe(followerId, followedId)` — нужен тесту ленты `FeedRepositoryJdbcTest` и будущему `DELETE /api/subscribes`.
  Реализация — `internal.application.service`, вызывается **в транзакции вызывающего** (создание заявки и подписки остаётся атомарным).

### `posts` — посты и лента

- **Данные:** таблица `posts`; лента — чтение `subscriptions JOIN posts`.
- **Классы:** `PostStatus`, `PostRepository`, `FeedRepository`, `PostRepositoryJdbc`, `FeedRepositoryJdbc`, `PostRow`, `PostPersistenceMapper`, `FeedPersistenceMapper`.
- **`allowedDependencies`:** `{}` — в коде `posts` от `subscriptions` не зависит.
- **`api`:** нет: сервиса и HTTP у постов пока нет, другие модули к ним не обращаются.
- **Исключение из правил:** SQL ленты читает чужую таблицу `subscriptions`. Это осознанный компромисс (одна выборка с `ORDER BY … LIMIT/OFFSET`, см. `../../process/Feed.md`). Он фиксируется в `docs/architecture.md`; при росте — проекция подписок в `posts` по событиям `subscriptions`.

### `friendships` — заявки в друзья и дружба

- **Данные:** таблицы `friendship_offer`, `friendships`.
- **Классы:** `FriendshipOffer`, `FriendshipOfferStatus`, `FriendshipStatus`, исключения `domain/friendshipoffer/exception/*` и `FriendshipAlreadyExistsException`, `CreateFriendshipOfferUseCase`, `FriendshipOfferService`, `FriendshipOfferApplicationMapper`, `FriendshipOfferRepository`, `FriendshipsRepository`, их адаптеры, row и мапперы, `FriendshipOfferController`, web-DTO, `FriendshipOfferWebMapper`, ошибки заявок из `GlobalExceptionHandler` / `ErrorCode`.
- **`allowedDependencies`:** `{"subscriptions::api"}` — создание заявки подписывает отправителя на получателя (принятие заявки позже создаст встречную подписку через тот же API).
- **`api`:** нет — другие модули к заявкам не обращаются.

### `system` — служебные эндпоинты

- **Классы:** `PingController` → `system/internal/infrastructure/web`.
- **`allowedDependencies`:** `{}`, `api` нет.
- Нужен, потому что контроллер в `shared` запрещён.

### `shared` — техническое ядро (не модуль Modulith)

| Пакет | Что переносится |
|---|---|
| `shared.config` | `ClockConfiguration` |
| `shared.web` | `ErrorResponse` (общее тело ошибки `{code, message}`) |
| `shared.exception` | обработчик `DataIntegrityViolationException` → `500 INTERNAL_ERROR` (выделяется из `GlobalExceptionHandler`) |

`PostgresErrors` в `shared` **не** переносится (решение 2026-10-16): копия в `internal.infrastructure.persistence.adapter` модулей `subscriptions` и `friendships`. Вынос в `shared` — позже, если понадобится.

## 6. Граф зависимостей модулей

```text
friendships ──► subscriptions::api
posts          (в коде — ни от кого; в SQL читает таблицу subscriptions)
system         (ни от кого)
все модули ──► shared (config, web, exception)
```

Циклов нет.

## 7. Раскладка классов внутри модулей (сводка решений)

| Сейчас | Станет |
|---|---|
| `adapter.out.persistence.row.*Row` | `<module>.internal.infrastructure.persistence.row` |
| `adapter.out.persistence.jdbc.*RepositoryJdbc` | `<module>.internal.infrastructure.persistence.adapter.*PersistenceAdapter` (переименование — шаг 2.2) |
| `adapter.out.persistence.mapper.*PersistenceMapper` | `<module>.internal.infrastructure.persistence.mapper` |
| `application.port.out.*Repository` (+ вложенные `*Request`/`*Response`) | `<module>.internal.application.port`; вложенные записи → `port.model` (шаг 2.2) |
| `application.port.in.CreateFriendshipOfferUseCase` + `FriendshipOfferService` | шаг 2.1: `internal.application.service`; шаг 2.2: класс-сценарий `usecase.CreateFriendshipOffer`, `CreateFriendshipOfferCommand` / `CreateFriendshipOfferResult` в `application.model`, интерфейс `port.in` удаляется |
| `application.mapper.*ApplicationMapper` | `<module>.internal.application.mapper` |
| `domain.<aggregate>.*` (статусы, агрегаты) | `<module>.internal.domain.model` |
| `domain.<aggregate>.exception.*` | `<module>.internal.domain.exception` |
| `adapter.in.web.*Controller` | `<module>.internal.infrastructure.web` |
| `adapter.in.web.dto.*WebRequest` / `*WebResponse` | `web.request` / `web.response` |
| `adapter.in.web.mapper.*WebMapper` | `<module>.internal.infrastructure.web.mapper` |

Тесты переносятся в зеркальные пакеты вместе с кодом.

## 8. Архитектурный тест — адаптация шаблона

- `Application` → `SocialMediaApiApplication`.
- Удалить `jpaEntitiesBelongToPersistence` и `springDataRepositoriesBelongToPersistence` (JPA и Spring Data нет на classpath). Вместо них — правило «`record` с суффиксом `Row` лежат только в `*.internal.infrastructure.persistence.row`».
- Удалить `<EXTERNAL_CLIENTS_PACKAGE>`, `<PLATFORM_TECH_PACKAGE>`, `shared.client`, `feign`, `io.temporal`.
- `LEGACY_PACKAGES` на переходный период: `adapter..`, `application..`, `domain..` (`config` перенесён в `shared.config` в итерации 1). Удалён на этапе 4.
- Пустые правила разрешены (`allowEmptyShould(true)`, `withOptionalLayers(true)`): пока модулей нет, правилам не на чем срабатывать. Удалены на этапе 4 — все правила срабатывают на реальных классах.
- Добавлено правило `sharedDoesNotDependOnModules` (этап 3).

## 9. Порядок переноса

| Итерация | Что | Остановка |
|---|---|---|
| 1 | Каркас: Modulith в `../../../pom.xml`, `detection-strategy: explicitly-annotated`, `ApplicationModulesTest` с `LEGACY_PACKAGES`, `shared` (`ClockConfiguration`, `ErrorResponse`), `docs/architecture.md`, раздел в `CLAUDE2.md` — **выполнено** | да |
| 2 | `subscriptions` (листовой): шаг 2.1 — **выполнен** → остановка → шаг 2.2 — **выполнен** | да, дважды |
| 3 | `posts` (зависит от `subscriptions` только в SQL и в тесте ленты): шаг 2.1 — **выполнен**, шаг 2.2 — **выполнен** | да, дважды |
| 4 | `friendships` (зависит от `subscriptions::api`), разделение `GlobalExceptionHandler`: шаг 2.1 — **выполнен**, шаг 2.2 — **выполнен** | да, дважды |
| 5 | `system` (`PingController`): шаг 2.1 — **выполнен**, шаг 2.2 не нужен (нет домена, сценариев, портов и данных) | да |
| 6 | Разбор `shared` — **выполнен** (см. раздел 11) | да |
| 7 | Завершение: `LEGACY_PACKAGES`, `allowEmptyShould`, `withOptionalLayers` удалены из теста; нет пустых пакетов; у всех `package-info` явные `allowedDependencies`; раздел «Переходный период» удалён из `../../architecture.md`; документация и `docs/md/shema.md` соответствуют коду — **выполнено** | да |

Коммиты итераций: `Modulith-N` (`Refactor` и `Refactor-2` в истории уже заняты). Коммиты делает разработчик.

## 10. Принятые решения

1. `subscriptions.api.unsubscribe` — **оставить** (нужен тесту ленты и будущему `DELETE /api/subscribes`).
2. `shared.persistence` — **нет**; `PostgresErrors` копируется в модули, вынос в `shared` — позже при необходимости.
3. Имена коммитов — `Modulith-N`.

## 11. Разбор `shared` (этап 3, 2026-10-16)

```text
shared/
├── config/     ClockConfiguration     ‹@Configuration›
├── exception/  GlobalExceptionHandler ‹@RestControllerAdvice›
└── web/        ErrorResponse          ‹record›
```

| Класс | Кто использует | Решение |
|---|---|---|
| `shared.config.ClockConfiguration` | бин `Clock` внедряется в `friendships` (`CreateFriendshipOffer`) | **остаётся**. Сейчас потребитель один, но правило «используется одним модулем → в модуль» здесь не подходит: `Clock` — один бин на приложение, сквозная техническая конфигурация. Если положить его в модуль, будущим сервисам `posts` и `subscriptions` понадобится свой бин (конфликт имён/типов) или зависимость от чужого модуля. |
| `shared.web.ErrorResponse` | `friendships` (`ErrorCode`, `FriendshipOfferExceptionHandler`), `shared.exception` | **остаётся**: формат тела ошибки `{code, message}` общий для всего API (OpenAPI). |
| `shared.exception.GlobalExceptionHandler` | все контроллеры (fallback после обработчиков модулей) | **остаётся**: общий ответ `500 INTERNAL_ERROR` не принадлежит ни одному модулю. |

Запрещённого содержимого (моделей хранения, репозиториев, контроллеров, шедулеров, бизнес-портов) в `shared` нет.
`shared` не импортирует бизнес-модули — теперь это проверяет правило `sharedDoesNotDependOnModules`
в `ApplicationModulesTest` (проверено на временном нарушающем классе: правило падает).
