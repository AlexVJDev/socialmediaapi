# Задание 05. REST-контроллер `POST /api/offer-friendships/create`

**Зависит от:** задания 04.

## Цель

Открыть use case по HTTP: успешный ответ `201` и валидация тела, из-за которой возникает часть ошибок `INVALID_FRIENDSHIP_OFFER_REQUEST`. Повтор (`409`) отдаёт обработчик ошибок из задания 06.

## Контекст

Контракт — `docs/api/paths/friendship-offer-create.yaml` (после задания 01). Контроллеры — в `adapter.in.web` (образец оформления — `PingController`). Web-мапперы — в `adapter.in.web.mapper` (`*WebMapper`, образец — `docs/md/examples/GoanPineMapper.java` / раздел «Маппинг» в `CLAUDE.md`). В проекте уже подключён `spring-boot-starter-validation`.

## Что сделать

1. Web-DTO в `adapter.in.web.dto` (records):
   - `CreateFriendshipOfferWebRequest(@NotNull UUID fromUserId, @NotNull UUID toUserId)`;
   - `CreateFriendshipOfferWebResponse(UUID id, UUID fromUserId, UUID toUserId, String status, Instant createdAt)` — ровно поля из контракта (без `updatedAt`).
2. `adapter.in.web.mapper.FriendshipOfferWebMapper` (MapStruct, `@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)`): `toUseCaseRequest(WebRequest)`, `toResponse(CreateFriendshipOfferResult)`.
3. `FriendshipOfferController`, `@RequestMapping("/api/offer-friendships")`, метод `@PostMapping("/create")` принимает `@Valid @RequestBody`:
   - результат → `ResponseEntity.status(201).body(...)`, других веток нет.
   Исключения в контроллере **не ловить**: повтор (`FriendshipOfferAlready{Pending,Accepted,Rejected}Exception` → `409`) и прочие ошибки — задача обработчика из задания 06.
4. Юнит-тест маппера `FriendshipOfferWebMapperTest` через `Mappers.getMapper(...)`.

## Критерии приёмки

- `./mvnw clean compile` без предупреждений MapStruct.
- Ручная проверка: `./mvnw spring-boot:run`, запрос с двумя разными UUID → `201`, повтор → `409 FRIENDSHIP_OFFER_ALREADY_PENDING` (после задания 06).
