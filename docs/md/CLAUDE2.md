# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Educational (job4j) Spring Boot 4.1 REST API for a social network: friendship offers, friendships and subscriptions. Java 17 (`java.version` in `pom.xml`). The project is **contract-first**: the API and business process are designed in `docs/` before being implemented. Currently the only implemented endpoint is `GET /api/ping` (`PingController`); everything else exists only as the OpenAPI contract and DB schema.

## Commands

On Windows use `mvnw.cmd` instead of `./mvnw`.

```shell
docker compose up -d postgres                # PostgreSQL 16 (settings from .env, see .env.example)
docker compose up -d swagger-ui              # Swagger UI for docs/api/openapi.yaml at http://localhost:8081
docker compose --profile tools run --rm openapi-validator   # validate the OpenAPI contract

./mvnw test                                  # run tests
./mvnw test -Dtest=SocialMediaApplicationTests#whenPingThenReturnServiceStatus   # single test
./mvnw verify                                # tests + Checkstyle (check goal) + JaCoCo report (target/site/jacoco)
./mvnw spring-boot:run                       # start the app
```

- Tests are `@SpringBootTest` and start the full context, including the datasource and Liquibase — **PostgreSQL must be running** for them to pass.
- The test class is `SocialMediaApplicationTests` but lives in `SocialMediaApiApplicationTests.java` (package-private class, so the name mismatch compiles); use the class name with `-Dtest`.
- Checkstyle runs in the `verify` phase with the plugin's default ruleset (no custom config file) and fails the build on violations.
- DB connection is configured via `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (defaults in `application.yml` match `.env.example`).

## Architecture

- **Persistence**: plain `spring-boot-starter-jdbc` (no JPA) + Liquibase. Master changelog `src/main/resources/db/db.changelog-master.yaml` includes numbered Liquibase *formatted SQL* files from `db/changes/` (`--liquibase formatted sql`, `--changeset student:NNN`, explicit `--rollback`). Each table gets one file for the table and a separate one for its indexes. New changesets must be added to the master changelog in order.
  - `offer_friendships` (UUID ids, `status` in `PENDING|ACCEPTED|REJECTED`, unique `(from_user_id, to_user_id)`, self-offer forbidden by CHECK) is implemented; `003`–`006` (friendships, subscriptions and their indexes) are still empty placeholders.
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
