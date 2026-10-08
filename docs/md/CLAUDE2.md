#  CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

##  Project 

Educational (job4j) Spring Boot 4.1 REST API for a social network: friendship offers, friendships and subscriptions. Java 17 (`java.version` in `pom.xml`). The project is **contract-first**: the API and business process are designed in `docs/` before being implemented. Currently the only implemented endpoint is `GET /api/ping` (`PingController`); the full DB schema and JDBC repositories for creating friendship offers, friendships and subscriptions exist, everything else exists only as the OpenAPI contract.



##  Commands 

On Windows use `mvnw.cmd` instead of `./mvnw`.

Не выполняй и не предлагай выполнять commit в git. Это пользователь будет выполнять исключительно самостоятельно.


```shell
docker compose up -d postgres                # PostgreSQL 17 for running the app (settings from .env, see .env.example)
docker compose up -d swagger-ui              # Swagger UI for docs/api/openapi.yaml at http://localhost:8081
docker compose --profile tools run --rm openapi-validator   # validate the OpenAPI contract

./mvnw test                                  # run tests (Docker must be running)
./mvnw test -Dtest=SocialMediaApiApplicationTests#whenPingThenReturnServiceStatus   # single test
./mvnw verify                                # tests + JaCoCo report (target/site/jacoco)
./mvnw spring-boot:run                       # start the app
```

- Tests are `@SpringBootTest` and start the full context, including the datasource and Liquibase, against a throwaway PostgreSQL container started by Testcontainers (`postgres:17`, wired via `@ServiceConnection`) — **Docker must be running**; the `docker compose` database is not used by tests. Either `@Import(TestcontainersConfiguration.class)` or declare a `@Container @ServiceConnection` field (as `OfferFriendshipRepositoryJdbcTest` does).
- Tests deliberately use PostgreSQL, not H2: the same Liquibase changesets and PostgreSQL-specific SQL (e.g. `INSERT ... RETURNING`) must be verified against the real database (rationale in `README.md`). Don't add H2.
- DB connection for the running app is configured via `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (defaults in `application.yml` match `.env.example`).

## Architecture

- **Packages** (hexagonal / ports and adapters, under `ru.job4j.socialmediaapi`); dependencies point inwards — `domain` depends on nothing, `application` only on `domain`, adapters on both:
  - `domain.<aggregate>` — domain model per aggregate, framework-free (e.g. `domain.offerfriendship.OfferFriendshipStatus`).
  - `application.port.out` — output ports, interfaces the application needs from infrastructure (e.g. `OfferFriendshipRepository`). Use cases / services and `application.port.in` go into `application` as well.
  - `adapter.in.web` — REST controllers (driving adapters).
  - `adapter.out.persistence` — driven persistence adapter, split into:
    - `entity` — table-mirroring `*Entity` records (`OfferFriendshipEntity`, `FriendshipEntity`, `SubscriptionEntity`). They are `public` only so that `jdbc` can use them; they must not be used outside `adapter.out.persistence` (ports and the domain use their own types).
    - `jdbc` — `JdbcTemplate` implementations of the output ports (`*RepositoryJdbc`): Request → Entity (mapper) → SQL → Entity (`RowMapper`) → Response (mapper). Their tests live in the same package under `src/test`.
    - `mapper` — MapStruct mappers between port Request/Response and entities (`*PersistenceMapper`, methods `toEntity` / `toResponse`).
- **Mapping** follows the «Маппинг» section below: MapStruct only (no hand-written object mapping in adapters/services; `RowMapper` for `ResultSet` is the exception), every mapper is an interface annotated `@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)` and lives next to the adapter of its layer; non-trivial mappers get a Spring-free unit test via `Mappers.getMapper(...)`. Generated code is in `target/generated-sources/annotations`.
- **Persistence**: plain `spring-boot-starter-jdbc` (no JPA) + Liquibase. A repository is an output port plus a `JdbcTemplate` adapter with native SQL (`OfferFriendshipRepository` / `OfferFriendshipRepositoryJdbc`, likewise `Friendship*` and `Subscription*`). Master changelog `src/main/resources/db/db.changelog-master.yaml` includes numbered Liquibase *formatted SQL* files from `db/changes/` (`--liquibase formatted sql`, `--changeset student:NNN`, explicit `--rollback`). Each table gets one file for the table and a separate one for its indexes. New changesets must be added to the master changelog in order.
  - Tables (all UUID ids, `TIMESTAMPTZ` timestamps): `offer_friendships` (`status` in `PENDING|ACCEPTED|REJECTED`, unique `(from_user_id, to_user_id)`, self-offer forbidden by CHECK); `friendships` (one row per pair stored in canonical order `first_user_id < second_user_id`, FK to the accepted offer); `subscriptions` (unique `(follower_id, followed_id)`, self-subscription forbidden).
- **API contract**: `docs/api/openapi.yaml` is the entry point; each path is a separate file in `docs/api/paths/*.yaml` referenced via `$ref`. Per-endpoint human docs live in `docs/api/*.md`. Endpoints: `/api/offer-friendships/{create,accept,reject,incoming,outgoing}`, `/api/friendships`, `/api/subscribes`, `/api/subscribes/followers`. Errors use `{code, message}` bodies (e.g. `USER_NOT_FOUND`, `OFFER_FRIENDSHIP_ALREADY_REJECTED`).
- **Business process** (`docs/process/`): friendship offer state machine `PENDING → ACCEPTED | REJECTED`, with the full transition table in `FriendshipRequest.md`.
  - Create offer → `PENDING` and the sender auto-subscribes to the recipient.
  - Accept → `ACCEPTED`, mutual friendship and the recipient's back-subscription are created atomically.
  - Reject → `REJECTED`, no friendship; the sender stays subscribed.
  - Operations are idempotent (repeating a transition that already reached its target state → `204`, no duplicates); invalid transitions (re-offering/accepting a rejected offer, rejecting an accepted one) → `409 Conflict`, each with its own exception type.
  - `friendship_transitions.puml` sketches the intended layering: `FriendRequestController → FriendRequestService → FriendRequestRepository`.
- **Known inconsistencies in the docs** — confirm with the user before implementing against them:
  - User/offer ids are `integer/int64` in the OpenAPI contract but `UUID` in the DB schema.
  - Repeated (idempotent) calls return `204` in the OpenAPI path files and `FriendshipRequest.md`, but `200 OK` with a body in `friendship_transitions.puml`; re-offering an `ACCEPTED` offer is `409` in the transition table but `204` in the idempotency rules.
  - Treat `docs/api/openapi.yaml` and its path files as the source of truth for the HTTP layer.
- Documentation (`docs/`) is written in Russian.


## Маппинг

Для преобразования объектов между слоями используется **MapStruct 1.6.3**. Ручной маппинг в контроллерах, сервисах и адаптерах не пишется: его выносят в маппер. Исключение — `RowMapper` для `ResultSet` в `*RepositoryJdbc`.

**Сборка (Maven).** Зависимость `org.mapstruct:mapstruct:${mapstruct.version}`. В `maven-compiler-plugin` → `annotationProcessorPaths` процессоры перечислены строго в порядке: `lombok` → `lombok-mapstruct-binding` (`${lombok-mapstruct-binding.version}`) → `mapstruct-processor`, затем `spring-boot-configuration-processor`. После указания `annotationProcessorPaths` Maven не ищет процессоры в classpath, поэтому любой новый процессор тоже добавляется в этот список.

**Образцы.** В первую очередь ориентироваться на код проекта:

| Образец | Что показывает |
|---|---|
| `adapter/out/persistence/mapper/OfferFriendshipPersistenceMapper` | Persistence-маппер: port Request → Entity → port Response, enum ↔ `String` |
| `OfferFriendshipPersistenceMapperTest` (`src/test`, тот же пакет) | Юнит-тест маппера через `Mappers.getMapper(...)` |

Для случаев, которых в проекте ещё нет, — образцы из другого проекта в `docs/md/examples`. Их модели и пакеты (`uz.kapitalbank...`, `infrastructure/...`) не копируются, файлы — только эталон оформления:

| Образец | Когда брать за основу |
|---|---|
| `LoanLineMapper` | Web-маппер с переименованием полей через `@Mapping(target, source)` |
| `ApplicationWebMapper` | Маппер, которому нужен Spring-бин или сложные выражения (abstract class + `@Autowired`). Объявление `componentModel = "spring"` в нём устарело — писать `SPRING` |
| `LoanTemplatePersistenceMapper` + `LoanTemplateCurrencyMapper` | `uses` + конвертер-`@Component` с бинами, value objects через `default`-методы, `expression` |

**Правила:**
- Объявление: `@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)`, `SPRING` — статический импорт `org.mapstruct.MappingConstants.ComponentModel.SPRING`. Общего `@MapperConfig` в проекте нет: параметры пишутся в каждом маппере.
- Маппер — `interface`. Abstract class используется, только если нужен внедрённый бин (см. `ApplicationWebMapper`).
- Вложенные мапперы и конвертеры подключаются через `uses = {...}` с `injectionStrategy = InjectionStrategy.CONSTRUCTOR`. Конвертер, которому нужны бины, — это обычный `@Component` (см. `LoanTemplateCurrencyMapper`).
- Отдельный маппер на каждую границу слоя. Маппер лежит в подпакете `mapper` рядом со своим адаптером (пакеты под `ru.job4j.socialmediaapi`):
  - `adapter.in.web.mapper` — web Request/Response ↔ Command/Query/Result из `application.port.in` (`*WebMapper`);
  - `adapter.out.persistence.mapper` — Request/Response выходного порта ↔ `*Entity` (`*PersistenceMapper`). Когда появится доменная модель агрегата — Entity ↔ Domain;
  - `application.mapper` — Domain → Result/View.
  - `domain` не зависит от мапперов и DTO; `*Entity` не выходят за пределы `adapter.out.persistence`.
- Имена методов: `toDomain`, `toEntity`, `toResponse`, `toCommand`, `toQuery`, `toDto`, `toXxx`.
- Несовпадающие поля задаются через `@Mapping(target, source)`, вложенные пути — через `source = "a.b.c"`. Поле игнорируется только явно: `@Mapping(target = "...", ignore = true)`.
- Enum ↔ `String` (статусы в БД хранятся строкой) MapStruct делает сам через `name()` / `valueOf()`; неизвестное значение из БД даёт `IllegalArgumentException`.
- Value objects (records) разворачиваются и собираются через `default`-методы маппера. `expression = "java(...)"` допустим только для однострочников.
- Маппер не содержит бизнес-логики и не обращается к репозиториям или внешним системам. Исключение — справочные конвертеры, подключённые через `uses`.

**Проверка:**
- `./mvnw clean compile` проходит без ошибок и предупреждений MapStruct об unmapped properties.
- Сгенерированная реализация лежит в `target/generated-sources/annotations`. При сомнениях смотреть туда.
- Для нетривиальных мапперов (переименования, enum ↔ `String`, `default`-методы, `expression`) пишется юнит-тест через `Mappers.getMapper(XxxMapper.class)`, без Spring-контекста, в том же пакете под `src/test` (см. `OfferFriendshipPersistenceMapperTest`). Если у маппера есть `uses` с бинами, конвертеры подставляются вручную или маппер тестируется в составе адаптера.
