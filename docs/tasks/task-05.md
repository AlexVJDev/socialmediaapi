# Задание 05. REST-контроллер `POST /api/offer-friendships/create`

**Зависит от:** задания 04.

## Цель

Открыть use case по HTTP: успешные ответы `201` и `204` и валидация тела, из-за которой возникает часть ошибок `INVALID_FRIENDSHIP_OFFER_REQUEST`.

## Контекст

Контракт — `docs/api/paths/friendship-offer-create.yaml` (после задания 01). Контроллеры — в `adapter.in.web` (образец оформления — `PingController`). Web-мапперы — в `adapter.in.web.mapper` (`*WebMapper`, образец — `docs/md/examples/GoanPineMapper.java` / раздел «Маппинг» в `CLAUDE.md`). В проекте уже подключён `spring-boot-starter-validation`.

## Что сделать

1. Web-DTO в `adapter.in.web.dto` (records):
   - `CreateOfferFriendshipWebRequest(@NotNull UUID fromUserId, @NotNull UUID toUserId)`;
   - `CreateOfferFriendshipWebResponse(UUID id, UUID fromUserId, UUID toUserId, String status, Instant createdAt)` — ровно поля из контракта (без `updatedAt`).
2. `adapter.in.web.mapper.OfferFriendshipWebMapper` (MapStruct, `@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)`): `toCommand(WebRequest)`, `toResponse(Created)`.
3. `OfferFriendshipController`, `@RequestMapping("/api/offer-friendships")`, метод `@PostMapping("/create")` принимает `@Valid @RequestBody`:
   - `Created` → `ResponseEntity.status(201).body(...)`;
   - `AlreadyPending` → `ResponseEntity.noContent().build()`.
   Исключения в контроллере **не ловить** — это задача обработчика из задания 06.
4. Юнит-тест маппера `OfferFriendshipWebMapperTest` через `Mappers.getMapper(...)`.

## Критерии приёмки

- `./mvnw clean compile` без предупреждений MapStruct.
- Ручная проверка: `./mvnw spring-boot:run`, запрос с двумя разными UUID → `201`, повтор → `204`.
