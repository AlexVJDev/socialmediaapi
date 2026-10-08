# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Educational (job4j) Spring Boot 4.1 REST API for a social network: friendship offers, friendships and subscriptions. Java 17 (`java.version` in `pom.xml`). The project is **contract-first**: the API and business process are designed in `docs/` before being implemented. Currently the only implemented endpoint is `GET /api/ping` (`PingController`); the full DB schema and a JDBC repository for creating friendship offers exist, everything else exists only as the OpenAPI contract.

## Commands

On Windows use `mvnw.cmd` instead of `./mvnw`.

```shell
docker compose up -d postgres                # PostgreSQL 17 for running the app (settings from .env, see .env.example)
docker compose up -d swagger-ui              # Swagger UI for docs/api/openapi.yaml at http://localhost:8081
docker compose --profile tools run --rm openapi-validator   # validate the OpenAPI contract

./mvnw test                                  # run tests (Docker must be running)
./mvnw test -Dtest=SocialMediaApiApplicationTests#whenPingThenReturnServiceStatus   # single test
./mvnw verify                                # tests + Checkstyle (check goal) + JaCoCo report (target/site/jacoco)
./mvnw spring-boot:run                       # start the app
```

- Tests are `@SpringBootTest` and start the full context, including the datasource and Liquibase, against a throwaway PostgreSQL container started by Testcontainers (`postgres:17`, wired via `@ServiceConnection`) — **Docker must be running**; the `docker compose` database is not used by tests. Either `@Import(TestcontainersConfiguration.class)` or declare a `@Container @ServiceConnection` field (as `OfferFriendshipRepositoryJdbcTest` does).
- Tests deliberately use PostgreSQL, not H2: the same Liquibase changesets and PostgreSQL-specific SQL (e.g. `INSERT ... RETURNING`) must be verified against the real database (rationale in `README.md`). Don't add H2.
- Checkstyle runs in the `verify` phase with the plugin's default ruleset (no custom config file) and fails the build on violations.
- DB connection for the running app is configured via `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (defaults in `application.yml` match `.env.example`).

## Architecture

- **Persistence**: plain `spring-boot-starter-jdbc` (no JPA) + Liquibase. Repositories are an interface plus a `JdbcTemplate` implementation with native SQL (`OfferFriendshipRepository` / `OfferFriendshipRepositoryJdbc`, entity records in the `repository` package, enums in `domain`). Master changelog `src/main/resources/db/db.changelog-master.yaml` includes numbered Liquibase *formatted SQL* files from `db/changes/` (`--liquibase formatted sql`, `--changeset student:NNN`, explicit `--rollback`). Each table gets one file for the table and a separate one for its indexes. New changesets must be added to the master changelog in order.
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
