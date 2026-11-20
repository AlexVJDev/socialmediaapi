# socialmediaapi
Educational project

## API contract

OpenAPI contract: [docs/api/openapi.yaml](docs/api/openapi.yaml)

Swagger UI (the application must be running): http://localhost:8080/swagger-ui.html
(redirects to `/swagger-ui/index.html`). The contract file itself is served at
http://localhost:8080/openapi/openapi.yaml

- `LIQUIBASE_CONTEXTS=dev` also loads the data of the contract examples
  (`db/dev/swagger-examples.sql`), so "Try it out" works with the example ids.
- `SWAGGER_UI_ENABLED=false` turns the UI off.

### Users for Swagger UI

Start with `LIQUIBASE_CONTEXTS=dev`, press **Authorize** and put a user id into `userIdHeader`
(header `X-User-Id`; a temporary replacement for the Keycloak token).

| User | `X-User-Id` | What for |
| --- | --- | --- |
| Alice (main) | `0b6f3c9e-6a1d-4c2e-9f5a-2d7b8e1c4a01` | **Every** request with the prefilled example values works on her behalf |
| Boris | `5e2a7d40-91c3-4b8f-a6e2-7c0d3f9b1e22` | Second point of view: recipient of the offer `9c4e1b77-3f2a-4d6c-8b0e-1a5f7d2c3e44` (Alice → Boris), 14 followers |

With Alice and the prefilled values every list request returns at least 10 items: incoming / outgoing
offers (12 / 14 `PENDING`, 10+ `ACCEPTED` and `REJECTED`), 21 friends, 46 subscriptions, 14 followers of Boris,
12 posts of Alice, 13 posts in her feed. `create`, `accept` and `reject` are prefilled with a free user and two
incoming offers of Alice, so they succeed on the first run.

Changes made through "Try it out" are reset on the next application start: the script first deletes
all data of the example users and then inserts it again (the result is the same, ids of new records differ).

The contract files `/openapi/**` are served with `Cache-Control: no-cache`, so after a contract change the browser
loads the new version. A copy cached before this setting may still be shown once: reload Swagger UI with
`Ctrl+F5` (or open it in a private window). Also restart the application after changing `docs/api`: the build
copies the contract into `static/openapi`.

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
(`TestcontainersConfiguration`, `FriendshipOfferRepositoryJdbcTest`) instead of
using an in-memory H2 database. The goal of the tests is to check the code
against the same database it runs on in production, and H2 would only imitate
it:

- **The same migrations.** Liquibase applies the very same SQL changesets from
  `src/main/resources/db/changes` in tests and in the application. With H2 we
  would either need a separate H2-compatible schema or would have to give up
  PostgreSQL-specific syntax, and the tests would no longer verify the real
  schema.
- **PostgreSQL-specific SQL.** The repositories use plain JDBC with native SQL,
  for example `INSERT ... RETURNING` in `FriendshipOfferRepositoryJdbc`. H2
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

1. **Container managed by JUnit** — `FriendshipOfferRepositoryJdbcTest`:
   ```java
   @Testcontainers
   @SpringBootTest
   class FriendshipOfferRepositoryJdbcTest {

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

### Code style (Checkstyle)

The rules are in `checkstyle.xml`; only hand-written code in `src/main/java`
and `src/test/java` is checked. Any violation fails the build, so
`./mvnw verify` (and CI) runs the check after the tests.

Check the code style without running the tests:
```shell
./mvnw checkstyle:check
```
Each violation is printed with its file, line and rule, and the build fails;
`You have 0 Checkstyle violations.` and `BUILD SUCCESS` mean the code is clean.

Build an HTML report without failing the build, then open
`target/reports/checkstyle.html` in a browser:
```shell
./mvnw checkstyle:checkstyle
```

### Test results

Last verified on 2026-10-08 (macOS, Docker 29.7.2, Maven 3.9.16 via the
wrapper, JDK 21, PostgreSQL `postgres:17` started by Testcontainers).

`FriendshipOfferRepositoryJdbcTest` was run twice in a row; each run is a
separate Maven invocation with its own fresh PostgreSQL container:
```shell
sh ./mvnw test -Dtest=FriendshipOfferRepositoryJdbcTest
sh ./mvnw test -Dtest=FriendshipOfferRepositoryJdbcTest
```

Run 1:
```text
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 11.05 s -- in ru.job4j.socialmediaapi.repository.FriendshipOfferRepositoryJdbcTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Run 2:
```text
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 12.44 s -- in ru.job4j.socialmediaapi.repository.FriendshipOfferRepositoryJdbcTest
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

## Reading posts and the feed

Two read-only endpoints of the `posts` module (contract:
[post-by-id.yaml](docs/api/paths/post-by-id.yaml) — `get`, [feed-get.yaml](docs/api/paths/feed-get.yaml)):

- `GET /api/v1/posts/{postId}` — a published post; `404 POST_NOT_FOUND` if it
  does not exist or is deleted.
- `GET /api/v1/feed?limit=...` with the header `X-User-Id: <reader id>` — published
  posts of the authors the reader is subscribed to, newest first. `limit` is 1..100 (default 20).
  The next page is requested with `afterCreatedAt` and `afterId` copied from
  `next` of the previous response; `next: null` means there are no more posts.

Posts can be created with `POST /api/v1/posts` ([post-create.yaml](docs/api/paths/post-create.yaml)),
but for a feed with several pages it is quicker to add test data to the local
database directly (the app must have been started once, so Liquibase created the
tables). A reader `…0001` subscribed to an author `…0002` with 15 posts
(`Post 1` … `Post 15`, one per hour on 2026-11-01, `Post 15` is the newest):
```shell
docker compose exec -T postgres psql -U postgres -d social_media <<'SQL'
INSERT INTO subscriptions (id, follower_id, followed_id, created_at)
VALUES (gen_random_uuid(),
        '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000002', now());
INSERT INTO posts (id, author_id, title, content, status, created_at, updated_at)
SELECT ('bbbbbbbb-0000-0000-0000-' || lpad(n::TEXT, 12, '0'))::UUID,
       '00000000-0000-0000-0000-000000000002',
       'Post ' || n,
       'Text of post ' || n,
       'PUBLISHED',
       TIMESTAMPTZ '2026-11-01T00:00:00Z' + make_interval(hours => n),
       TIMESTAMPTZ '2026-11-01T00:00:00Z' + make_interval(hours => n)
FROM generate_series(1, 15) n;
SQL
```
For a large data set (2 million posts, reader `55555555-5555-5555-5555-555555555555`)
use `src/main/resources/db/changes/0091-sql-examples.sql` instead (see the
comments at the top of that file).

Then, with the application running (`./mvnw spring-boot:run`):

1. Get a post by id:
   ```shell
   curl -s http://localhost:8080/api/v1/posts/bbbbbbbb-0000-0000-0000-000000000015
   ```
   ```json
   {"id":"bbbbbbbb-0000-0000-0000-000000000015","authorId":"00000000-0000-0000-0000-000000000002",
    "title":"Post 15","content":"Text of post 15","status":"PUBLISHED",
    "createdAt":"2026-11-01T15:00:00Z","updatedAt":"2026-11-01T15:00:00Z"}
   ```

2. First page of the feed, ten posts per page:
   ```shell
   curl -s -H "X-User-Id: 00000000-0000-0000-0000-000000000001" \
     "http://localhost:8080/api/v1/feed?limit=10"
   ```
   Returns the posts `Post 15` … `Post 6` (newest first) and
   `"next":{"createdAt":"2026-11-01T06:00:00Z","id":"bbbbbbbb-0000-0000-0000-000000000006"}`.

3. Next page — the cursor is the `next` of the previous response:
   ```shell
   curl -s -H "X-User-Id: 00000000-0000-0000-0000-000000000001" \
     "http://localhost:8080/api/v1/feed?limit=10&afterCreatedAt=2026-11-01T06:00:00Z&afterId=bbbbbbbb-0000-0000-0000-000000000006"
   ```
   Returns the remaining posts `Post 5` … `Post 1` and `"next":null` — the feed
   has no more posts.

Errors use the `{status, detail, code}` body
([api-error-response.yaml](docs/api/schemas/api-error-response.yaml)): a missing
post gives `404 POST_NOT_FOUND`; an id that is not a UUID, a `limit` outside
1..100 or only one of `afterCreatedAt`/`afterId` gives `400 VALIDATION_ERROR`;
a missing `X-User-Id` header or one that is not a UUID gives `401 UNAUTHORIZED`.
