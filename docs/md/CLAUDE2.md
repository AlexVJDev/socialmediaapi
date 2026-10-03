# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Educational (job4j) Spring Boot 4.1 / Java 21 REST API for a social network: friendship offers, friendships and subscriptions. The project is **contract-first**: the API and business process are designed in `docs/` before being implemented. Currently the only implemented endpoint is `GET /api/ping` (`PingController`); everything else exists only as the OpenAPI contract.

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
- Checkstyle runs in the `verify` phase with the plugin's default ruleset (no custom config file) and fails the build on violations.
- DB connection is configured via `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (defaults in `application.yml` match `.env.example`).

## Architecture

- **Persistence**: plain `spring-boot-starter-jdbc` (no JPA) + Liquibase. Master changelog: `src/main/resources/db/db.changelog-master.yaml` — currently an empty `include:` stub (SQL changesets were removed); new changesets must be added there.
- **API contract**: `docs/api/openapi.yaml` is the entry point; each path is a separate file in `docs/api/paths/*.yaml` referenced via `$ref`. Per-endpoint human docs live in `docs/api/*.md`. Endpoints: `/api/offer-friendships/{create,accept,reject,incoming,outgoing}`, `/api/friendships`, `/api/subscribes`, `/api/subscribes/followers`. Errors use `{code, message}` bodies (e.g. `USER_NOT_FOUND`, `OFFER_FRIENDSHIP_ALREADY_REJECTED`).
- **Business process** (`docs/process/`, in Russian): friendship offer state machine `PENDING → ACCEPTED | REJECTED`.
  - Create offer → `PENDING` and the sender auto-subscribes to the recipient.
  - Accept → `ACCEPTED`, mutual friendship is created and the recipient subscribes back.
  - Reject → `REJECTED`, no friendship; the sender stays subscribed.
  - Operations are idempotent (repeating the same transition creates no duplicates); invalid transitions (re-creating/accepting a rejected offer, rejecting an accepted one) → `409 Conflict`.
  - `friendship_transitions.puml` sketches the intended layering: `FriendRequestController → FriendRequestService → FriendRequestRepository`.
- When docs disagree (e.g. the response for a duplicate create is `204` in the OpenAPI path file but `200` in the PlantUML diagram), treat `docs/api/openapi.yaml` and its path files as the source of truth, and confirm with the user.
- Documentation is written in Russian.
