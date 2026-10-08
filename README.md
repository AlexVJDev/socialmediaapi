# socialmediaapi
Educational project

## API contract

OpenAPI contract: [docs/api/openapi.yaml](docs/api/openapi.yaml)

## PostgreSQL

The database runs in Docker with the following default settings:

- image: `postgres:17`
- database: `social_media`
- schema: `public`
- user: `postgres`
- password: `password`
- port: `5432`

Start PostgreSQL:
```shell
docker compose up -d
```

Check container status:
```shell
docker compose ps
```

View PostgreSQL logs (useful for diagnosing startup and connection errors):
```shell
docker compose logs postgres
```

Stop PostgreSQL and remove its container and network while preserving the
database volume:
```shell
docker compose down
```

Stop PostgreSQL and delete its volume with all database data. Use this command
only when the database needs to be initialized from scratch:
```shell
docker compose down -v
```

The PostgreSQL data directory is tied to the major version. If the volume was
created by an older image (for example, `postgres:16`), the `postgres:17`
container will not start and the logs will report that the database files are
incompatible with the server. For local development data, recreate the volume
with `docker compose down -v` and `docker compose up -d`; to keep the data,
dump it with `pg_dump` from the old version first and restore it afterwards.

## Why tests run on PostgreSQL, not H2

Tests start a real PostgreSQL in Docker via Testcontainers
(`TestcontainersConfiguration`, `OfferFriendshipRepositoryJdbcTest`) instead of
using an in-memory H2 database. The goal of the tests is to check the code
against the same database it runs on in production, and H2 would only imitate
it:

- **The same migrations.** Liquibase applies the very same SQL changesets from
  `src/main/resources/db/changes` in tests and in the application. With H2 we
  would either need a separate H2-compatible schema or would have to give up
  PostgreSQL-specific syntax, and the tests would no longer verify the real
  schema.
- **PostgreSQL-specific SQL.** The repositories use plain JDBC with native SQL,
  for example `INSERT ... RETURNING` in `OfferFriendshipRepositoryJdbc`. H2
  does not support `RETURNING` even in `MODE=PostgreSQL`, so such a query
  would fail in tests or would have to be rewritten only for them.
- **Types and constraints.** The schema relies on `UUID`, `TIMESTAMPTZ`,
  `CHECK` and `UNIQUE` constraints (`status` values, no self-offers, one offer
  per pair of users, canonical user order in `friendships`). The business rules
  of the friendship process are enforced by these constraints, so the tests
  must check how PostgreSQL itself compares values, stores time zones and
  reports violations — H2 compatibility mode does this differently or only
  partially.
- **No false positives.** A test that passes on H2 but fails on PostgreSQL
  gives false confidence; a green build should mean the code works with the
  real database.

The price is that **Docker must be running** for `./mvnw test`. You do not need
to start the database from `docker compose` for tests: Testcontainers starts a
fresh, isolated PostgreSQL container for the test run and removes it afterwards,
so tests do not depend on, or change, the data in your local database.

## Test annotations

In short: `@SpringBootTest` starts the application, `@Testcontainers` and
`@Container` start a Docker container with the database, and
`@ServiceConnection` connects the application to it.

- **`@SpringBootTest`** (Spring Boot) starts the full application context
  before the tests, almost as `./mvnw spring-boot:run` does: all beans, the
  datasource, Liquibase migrations and `JdbcTemplate`. Real beans can then be
  injected into the test with `@Autowired`. Spring caches the context, so test
  classes with the same configuration reuse it instead of starting a new one.
- **`@Testcontainers`** (JUnit 5 extension from `testcontainers-junit-jupiter`)
  finds the fields marked with `@Container` in the test class and manages their
  lifecycle: starts the containers before the tests and stops them afterwards.
  Without such fields it does nothing.
- **`@Container`** marks a field whose container the `@Testcontainers`
  extension manages. A `static` field gets one container for all tests of the
  class; an instance field gets a new container for every test, which is more
  isolated but much slower.
- **`@ServiceConnection`** (`spring-boot-testcontainers`) lets Spring Boot read
  the container's real connection details (Docker assigns a random port) and
  build `JdbcConnectionDetails` from them. These override `spring.datasource.*`
  from `application.yml`, so tests use the container instead of
  `localhost:5432`. It replaces a hand-written `@DynamicPropertySource` method.

The project uses two ways to wire the container:

1. **Container managed by JUnit** — `OfferFriendshipRepositoryJdbcTest`:
   ```java
   @Testcontainers
   @SpringBootTest
   class OfferFriendshipRepositoryJdbcTest {

       @Container
       @ServiceConnection
       static PostgreSQLContainer postgres =
               new PostgreSQLContainer("postgres:17");
   }
   ```
   JUnit starts the `static` container, `@SpringBootTest` creates the context,
   `@ServiceConnection` points the datasource at the container, Liquibase
   creates the schema in the empty database, and the container is stopped
   after the class's tests.
2. **Container as a Spring bean** — `SocialMediaApiApplicationTests`:
   ```java
   @SpringBootTest
   @Import(TestcontainersConfiguration.class)
   class SocialMediaApiApplicationTests {
   }
   ```
   `TestcontainersConfiguration` declares the container as a bean with
   `@ServiceConnection`. Spring starts and stops it together with the context,
   so `@Testcontainers` and `@Container` are not needed, and one container can
   be shared by several test classes.

Because the two classes are configured differently, one test run starts two
Spring contexts and two containers. This is not an error, but it makes the
tests slower; with `@Import(TestcontainersConfiguration.class)` in both classes
there would be one context and one container.

## Test run result example:

UPDATE SUMMARY
Run:                          6
Previously run:               0
Filtered out:                 0
-------------------------------
Total change sets:            6
Update summary generated
Update command completed successfully.
Liquibase: Update has been successful. Rows affected: 0
Successfully released change log lock
Command execution complete


## Application

Run the tests on Linux or macOS (Docker must be running):
```shell
./mvnw test
```

If the shell reports `./mvnw: Permission denied`, the wrapper is not
executable (it is stored in git with mode `100644`); run it through `sh`
instead:
```shell
sh ./mvnw test
```

### Test results

Last verified on 2026-10-08 (macOS, Docker 29.7.2, Maven 3.9.16 via the
wrapper, JDK 25, PostgreSQL `postgres:17` started by Testcontainers).

`OfferFriendshipRepositoryJdbcTest` was run twice in a row; each run is a
separate Maven invocation with its own fresh PostgreSQL container:
```shell
sh ./mvnw test -Dtest=OfferFriendshipRepositoryJdbcTest
sh ./mvnw test -Dtest=OfferFriendshipRepositoryJdbcTest
```

Run 1:
```text
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 11.05 s -- in ru.job4j.socialmediaapi.repository.OfferFriendshipRepositoryJdbcTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Run 2:
```text
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 12.44 s -- in ru.job4j.socialmediaapi.repository.OfferFriendshipRepositoryJdbcTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Both runs passed with the same result: 4 tests, no failures or errors.

Start the application on Linux or macOS:
```shell
./mvnw spring-boot:run
```

Stop:
```text
Ctrl + C
```
