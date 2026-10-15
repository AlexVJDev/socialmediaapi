#  CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

##  Project 

Educational (job4j) Spring Boot 4.1 REST API for a social network: friendship offers, friendships and subscriptions. Java 17 (`java.version` in `pom.xml`). The project is **contract-first**: the API and business process are designed in `docs/` before being implemented. Implemented so far: `GET /api/ping` (`PingController`) and `POST /api/offer-friendships/create` (`FriendshipOfferController` → `FriendshipOfferService`). The full DB schema and JDBC repositories for creating friendship offers, friendships and subscriptions exist; accept/reject and all list endpoints exist only as the OpenAPI contract. Step-by-step tasks are in `docs/tasks/task-*.md` (and `task_new.md` for review fixes).



##  Commands 

On Windows use `mvnw.cmd` instead of `./mvnw`.

Не выполняй и не предлагай выполнять commit в git. Это пользователь будет выполнять исключительно самостоятельно.


```shell
docker compose up -d postgres                # PostgreSQL 17 for running the app (settings from .env, see .env.example)
docker compose up -d swagger-ui              # Swagger UI for docs/api/openapi.yaml at http://localhost:8081
docker compose --profile tools run --rm openapi-validator   # validate the OpenAPI contract

./mvnw test                                  # run tests (Docker must be running)
./mvnw test -Dtest=SocialMediaApiApplicationTests#whenPingThenReturnServiceStatus   # single test
./mvnw verify                                # tests + JaCoCo report (target/site/jacoco) + Checkstyle (checkstyle.xml, fails the build)
./mvnw spring-boot:run                       # start the app
```

- CI: `.github/workflows/maven.yml` runs `./mvnw -B verify` on JDK 17 for pushes and PRs to `main`, so Checkstyle violations (e.g. missing Javadoc `@param`) break the build.
- Integration tests (`*RepositoryJdbcTest`, `FriendshipOfferControllerTest`, `FriendshipOfferServiceConcurrencyTest`, `SocialMediaApiApplicationTests`) are `@SpringBootTest` and start the full context, including the datasource and Liquibase, against a throwaway PostgreSQL container started by Testcontainers (`postgres:17`, wired via `@ServiceConnection`) — **Docker must be running**; the `docker compose` database is not used by tests. Either `@Import(TestcontainersConfiguration.class)` or declare a `@Container @ServiceConnection` field (as `FriendshipOfferRepositoryJdbcTest` does). Unit tests run without Spring and Docker: domain (`FriendshipOfferTest`), service with Mockito (`FriendshipOfferServiceTest`), mappers via `Mappers.getMapper(...)`, `GlobalExceptionHandlerTest`.
- Tests deliberately use PostgreSQL, not H2: the same Liquibase changesets and PostgreSQL-specific SQL (e.g. `INSERT ... RETURNING`) must be verified against the real database (rationale in `README.md`). Don't add H2.
- DB connection for the running app is configured via `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (defaults in `application.yml` match `.env.example`, host port `5435`). `.env` is read only by `docker compose`, not by the app: when changing `POSTGRES_PORT`, change the default `DB_URL` too.
- Changesets already applied to the local `docker compose` DB must not be edited (Liquibase fails on the checksum); if one was edited, the local schema has to be reset by the user (`DROP SCHEMA public CASCADE; CREATE SCHEMA public;` or `docker compose down -v`).
- If the project is also open in VS Code, its Java extension compiles into the same `target/classes` and breaks Maven builds ("Unresolved compilation problems"); build from a copy of the project or ask the user to close it there.

## Architecture

- **Packages** (hexagonal / ports and adapters, under `ru.job4j.socialmediaapi`); dependencies point inwards — `domain` depends on nothing, `application` only on `domain`, adapters on both:
  - `domain.<aggregate>` — domain model per aggregate, framework-free: `friendshipoffer` (`FriendshipOffer` record + `FriendshipOfferStatus`), `friendship` (`FriendshipStatus`), `subscription` (exceptions only). Domain exceptions live in `domain.<aggregate>.exception`, extend `RuntimeException`, English message via `"...".formatted(...)`. Invariants are checked in the domain: `FriendshipOffer`'s compact constructor rejects `null` fields and an offer to oneself (`FriendshipOfferToSelfException`), `FriendshipOffer.create(from, to, now)` generates the id and always sets `PENDING`. DB constraints only back them up.
  - `application.port.in` — use cases with nested `*UseCaseRequest` (named so as not to clash with `port.out` `*Request`) / `*Result` records (`CreateFriendshipOfferUseCase`: `CreateFriendshipOfferResult`); services implementing them live in `application` (`FriendshipOfferService`, `@Transactional`). Time comes from the `Clock` bean (`config.ClockConfiguration`, UTC), not `Instant.now()`.
  - `application.port.out` — output ports, interfaces the application needs from infrastructure, with nested `*Request` / `*Response` records (`FriendshipOfferRepository`, `FriendshipsRepository`, `SubscriptionRepository`). Javadoc `@throws` lists only the `*AlreadyExistsException` the adapter really throws.
  - `application.mapper` — MapStruct mappers Domain → port out Request and port out Response → use case Result (`FriendshipOfferApplicationMapper`).
  - `adapter.in.web` — REST controllers (driving adapters), `dto` (web Request/Response records with Bean Validation), `mapper` (`*WebMapper`), `error`:
    - `GlobalExceptionHandler` (`@RestControllerAdvice`) maps exceptions to `ErrorResponse(code, message)`; `ErrorCode` enum holds the HTTP status and Russian message, the constant name is the `code`. Invalid body / `FriendshipOfferToSelfException` → `400 INVALID_FRIENDSHIP_OFFER_REQUEST`; `FriendshipOfferAlready{Pending,Accepted,Rejected}Exception` → `409 FRIENDSHIP_OFFER_ALREADY_{PENDING,ACCEPTED,REJECTED}` (the response is built by `ErrorCode.response()`); unexpected `DataIntegrityViolationException` → logged (SLF4J) + `500 INTERNAL_ERROR` without DB details.
  - `adapter.out.persistence` — driven persistence adapter, split into:
    - `row` — table-mirroring `*Row` records (`FriendshipOfferRow`, `FriendshipsRow`, `SubscriptionRow`). Named `*Row`, not `*Entity`: in DDD an entity is a domain object such as `FriendshipOffer`. They are `public` only so that `jdbc` can use them; they must not be used outside `adapter.out.persistence` (ports and the domain use their own types).
    - `jdbc` — `JdbcTemplate` implementations of the output ports (`*RepositoryJdbc`): Request → Row (mapper) → SQL → Row (`RowMapper`) → Response (mapper). Their tests live in the same package under `src/test`.
      - JDBC-адаптер (прямо в `catch` метода создания, без отдельного `translate`; имя констрейнта — через package-private `PostgresErrors.constraint`) переводит в доменное исключение только нарушение уникального ключа («запись уже есть», `*AlreadyExistsException`); прочие `DataIntegrityViolationException` (CHECK, NOT NULL, FK) пробрасываются как есть — это ошибка в коде, `GlobalExceptionHandler` логирует её и отвечает `500` (`INTERNAL_ERROR`). PostgreSQL прерывает транзакцию после unique violation, поэтому если вызывающему нужно продолжить работу после дубликата, метод адаптера помечается `@Transactional(propagation = NESTED)` (savepoint откатывает только `INSERT`; так сделано в `FriendshipOfferRepositoryJdbc.createFriendshipOffer` → `FriendshipOfferAlreadyExistsException`: сервис после него читает существующую заявку и бросает исключение по её статусу); если дубликат — не ошибка, используется `INSERT ... ON CONFLICT DO NOTHING` и метод `*IfAbsent` с `Optional` (`SubscriptionRepository.createSubscriptionIfAbsent`).
    - `mapper` — MapStruct mappers between port Request/Response and rows (`*PersistenceMapper`, methods `toRow` / `toResponse`).
- **Mapping** follows the «Маппинг» section below: MapStruct only (no hand-written object mapping in adapters/services; `RowMapper` for `ResultSet` is the exception), every mapper is an interface annotated `@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)` and lives next to the adapter of its layer; non-trivial mappers get a Spring-free unit test via `Mappers.getMapper(...)`. Generated code is in `target/generated-sources/annotations`.
- **Persistence**: plain `spring-boot-starter-jdbc` (no JPA) + Liquibase. A repository is an output port plus a `JdbcTemplate` adapter with native SQL (`FriendshipOfferRepository` / `FriendshipOfferRepositoryJdbc`, `SubscriptionRepository` / `SubscriptionRepositoryJdbc`; for the `friendships` table the port, adapter and row are named in plural — `FriendshipsRepository` / `FriendshipsRepositoryJdbc` / `FriendshipsRow`, but the mapper is `FriendshipPersistenceMapper`). Master changelog `src/main/resources/db/db.changelog-master.yaml` includes numbered Liquibase *formatted SQL* files from `db/changes/` (`--liquibase formatted sql`, `--changeset student:NNN`, explicit `--rollback`). Each table gets one file for the table and a separate one for its indexes. New changesets must be added to the master changelog in order.
  - Tables (all UUID ids, `TIMESTAMPTZ` timestamps): `friendship_offer` (`status` in `PENDING|ACCEPTED|REJECTED`, unique `(from_user_id, to_user_id)`, self-offer forbidden by CHECK); `friendships` (one row per pair stored in canonical order `first_user_id < second_user_id` — PostgreSQL compares UUIDs unsigned, i.e. like their string form, not like `UUID.compareTo`; FK `friendship_offer_id` to the accepted offer, `status` in `ACTIVE|DELETED`, default `ACTIVE`); `subscriptions` (unique `(follower_id, followed_id)`, self-subscription forbidden).
- **API contract**: `docs/api/openapi.yaml` is the entry point; each path is a separate file in `docs/api/paths/*.yaml` referenced via `$ref`. Per-endpoint human docs live in `docs/api/*.md`. Endpoints: `/api/offer-friendships/{create,accept,reject,incoming,outgoing}`, `/api/friendships`, `/api/subscribes`, `/api/subscribes/followers`. Errors use `{code, message}` bodies (e.g. `USER_NOT_FOUND`, `FRIENDSHIP_OFFER_ALREADY_REJECTED`).
- **Business process** (`docs/process/`): friendship offer state machine `PENDING → ACCEPTED | REJECTED`, with the full transition table in `FriendshipRequest.md`.
  - Create offer → `PENDING` and the sender auto-subscribes to the recipient.
  - Accept → `ACCEPTED`, mutual friendship and the recipient's back-subscription are created atomically.
  - Reject → `REJECTED`, no friendship; the sender stays subscribed.
  - Repeats never create duplicates. Create inserts first (no preliminary lookup); on a unique key violation the service reads the existing offer and throws the exception of its status (if the offer cannot be read, the original `FriendshipOfferAlreadyExistsException` is rethrown; `GlobalExceptionHandler` logs it at WARN and answers `204` without a body), which `GlobalExceptionHandler` maps to `409`: `PENDING` → `FRIENDSHIP_OFFER_ALREADY_PENDING`, `ACCEPTED` → `FRIENDSHIP_OFFER_ALREADY_ACCEPTED`, `REJECTED` → `FRIENDSHIP_OFFER_ALREADY_REJECTED`. Accept/reject of an offer that is no longer `PENDING` (including a repeat) → `409` per the OpenAPI contract and `friendship_transitions.puml`. Each error case has its own exception type.
  - `friendship_transitions.puml` is the sequence diagram for create/accept/reject (API ↔ DB, transactions, response codes); implementation steps for create are in `docs/tasks/task-*.md`.
- **Known inconsistencies in the docs** — confirm with the user before implementing against them:
  - User/offer ids are `UUID` in the DB schema and in `/api/offer-friendships/create`, but still `integer/int64` in the other OpenAPI path files.
  - Repeated accept of an `ACCEPTED` offer and repeated reject of a `REJECTED` offer return `204` in `FriendshipRequest.md`, but `409` in the OpenAPI path files (`friendship-offer-accept.yaml`, `friendship-offer-reject.yaml`) and in `friendship_transitions.puml`.
  - `500 INTERNAL_ERROR` (returned by `GlobalExceptionHandler`) is not described in the OpenAPI contract.
  - `404 USER_NOT_FOUND` is in the contract, but there is no users table or user lookup yet.
  - `FriendshipRequestNotFoundException` and `SubscriptionToSelfException` exist but are not thrown anywhere yet (the former is planned for accept/reject in `FriendshipRequest.md`).
  - Treat `docs/api/openapi.yaml` and its path files as the source of truth for the HTTP layer.
- Documentation (`docs/`) is written in Russian.


## Маппинг

Для преобразования объектов между слоями используется **MapStruct 1.6.3**. Ручной маппинг в контроллерах, сервисах и адаптерах не пишется: его выносят в маппер. Исключение — `RowMapper` для `ResultSet` в `*RepositoryJdbc`.

**Сборка (Maven).** Зависимость `org.mapstruct:mapstruct:${mapstruct.version}`. В `maven-compiler-plugin` → `annotationProcessorPaths` процессоры перечислены строго в порядке: `lombok` → `lombok-mapstruct-binding` (`${lombok-mapstruct-binding.version}`) → `mapstruct-processor`, затем `spring-boot-configuration-processor`. После указания `annotationProcessorPaths` Maven не ищет процессоры в classpath, поэтому любой новый процессор тоже добавляется в этот список.

**Образцы.** В первую очередь ориентироваться на код проекта:

| Образец | Что показывает |
|---|---|
| `adapter/out/persistence/mapper/FriendshipOfferPersistenceMapper` | Persistence-маппер: port Request → Row → port Response, enum ↔ `String` |
| `adapter/in/web/mapper/FriendshipOfferWebMapper` | Web-маппер: web Request → UseCaseRequest, Result → web Response |
| `application/mapper/FriendshipOfferApplicationMapper` | Domain (`FriendshipOffer`) → port Request, port Response → Result |
| `FriendshipOfferPersistenceMapperTest` (`src/test`, тот же пакет) | Юнит-тест маппера через `Mappers.getMapper(...)` |

Для случаев, которых в проекте ещё нет, — образцы из другого проекта в `docs/md/examples`. Их модели и пакеты (`uz.kapitalbank...`, `infrastructure/...`) не копируются, файлы — только эталон оформления. Имена файлов не совпадают с именами классов внутри (класс указан первым, файл — в скобках):

| Образец | Когда брать за основу |
|---|---|
| `LoanLineMapper` (`GoanPineMapper.java`) | Web-маппер с переименованием полей через `@Mapping(target, source)` |
| `ApplicationWebMapper` (`ApplicationWebMapper.java`) | Маппер, которому нужен Spring-бин или сложные выражения (abstract class + `@Autowired`). Объявление `componentModel = "spring"` в нём устарело — писать `SPRING` |
| `LoanTemplatePersistenceMapper` + `LoanTemplateCurrencyMapper` (`GoanTemplatePersistenceMapper.java`, `GoanTemplateCurMapper.java`) | `uses` + конвертер-`@Component` с бинами, value objects через `default`-методы, `expression` |
| `LoanLineAbsClientMapper` + тест (`GoanPineAbsClientMapper.java`, `GoanPineAbsClientMapperTest.java`) | Integration-маппер: DTO внешнего клиента ↔ модель, и его юнит-тест |

**Правила:**
- Объявление: `@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)`, `SPRING` — статический импорт `org.mapstruct.MappingConstants.ComponentModel.SPRING`. Общего `@MapperConfig` в проекте нет: параметры пишутся в каждом маппере.
- Маппер — `interface`. Abstract class используется, только если нужен внедрённый бин (см. `ApplicationWebMapper`).
- Вложенные мапперы и конвертеры подключаются через `uses = {...}` с `injectionStrategy = InjectionStrategy.CONSTRUCTOR`. Конвертер, которому нужны бины, — это обычный `@Component` (см. `LoanTemplateCurrencyMapper`).
- Отдельный маппер на каждую границу слоя. Маппер лежит в подпакете `mapper` рядом со своим адаптером (пакеты под `ru.job4j.socialmediaapi`):
  - `adapter.in.web.mapper` — web Request/Response ↔ UseCaseRequest/Result из `application.port.in` (`*WebMapper`);
  - `adapter.out.persistence.mapper` — Request/Response выходного порта ↔ `*Row` (`*PersistenceMapper`). Порты пока принимают Request, а не доменную модель; если порт перейдёт на Domain — маппер станет Row ↔ Domain;
  - `application.mapper` — Domain → Request выходного порта (`toRequest`) и Response порта / Domain → Result/View (`toResult`).
  - `domain` не зависит от мапперов и DTO; `*Row` не выходят за пределы `adapter.out.persistence`.
- Имена методов: `toDomain`, `toRow`, `toResponse`, `toUseCaseRequest`, `toDto`, `toXxx`.
- Несовпадающие поля задаются через `@Mapping(target, source)`, вложенные пути — через `source = "a.b.c"`. Поле игнорируется только явно: `@Mapping(target = "...", ignore = true)`.
- Enum ↔ `String` (статусы в БД хранятся строкой) MapStruct делает сам через `name()` / `valueOf()`; неизвестное значение из БД даёт `IllegalArgumentException`.
- Value objects (records) разворачиваются и собираются через `default`-методы маппера. `expression = "java(...)"` допустим только для однострочников.
- Маппер не содержит бизнес-логики и не обращается к репозиториям или внешним системам. Исключение — справочные конвертеры, подключённые через `uses`.

**Проверка:**
- `./mvnw clean compile` проходит без ошибок и предупреждений MapStruct об unmapped properties.
- Сгенерированная реализация лежит в `target/generated-sources/annotations`. При сомнениях смотреть туда.
- Для нетривиальных мапперов (переименования, enum ↔ `String`, `default`-методы, `expression`) пишется юнит-тест через `Mappers.getMapper(XxxMapper.class)`, без Spring-контекста, в том же пакете под `src/test` (см. `FriendshipOfferPersistenceMapperTest`). Если у маппера есть `uses` с бинами, конвертеры подставляются вручную или маппер тестируется в составе адаптера.

