   # Задание 03. Поиск существующей заявки в выходном порте

**Зависит от:** задания 02.

## Цель

Дать сервису возможность узнать, есть ли уже заявка в направлении Алиса → Борис и в каком она статусе — без этого нельзя различить «создать», «повтор `PENDING`» и ошибки `ACCEPTED`/`REJECTED`.

## Контекст

Схема: `Api -> DB : найти заявку Алиса -> Борис`. Проверка относится только к этому направлению (встречная заявка Борис → Алиса — отдельная строка, `uq_offer_friendships_users` это допускает).
Слой порта: `application.port.out.OfferFriendshipRepository` (Request/Response — вложенные records). Адаптер: `FriendshipOfferRepositoryJdbc` (SQL → Entity через `RowMapper` → Response через MapStruct-маппер). Индекс `uq_offer_friendships_users (from_user_id, to_user_id)` уже покрывает поиск.

## Что сделать

1. В `OfferFriendshipRepository` добавить:
   ```java
   Optional<FindOfferFriendshipResponse> findByUsers(UUID fromUserId, UUID toUserId);

   record FindOfferFriendshipResponse(
           UUID id, UUID fromUserId, UUID toUserId,
           OfferFriendshipStatus status, Instant createdAt, Instant updatedAt
   ) { }
   ```
   (Если структура совпадёт с `CreateOfferFriendshipResponse` один в один — допустимо переиспользовать его вместо нового record; решение зафиксировать в Javadoc.)
2. В `FriendshipOfferRepositoryJdbc` реализовать метод:
   - SQL: `SELECT id, from_user_id, to_user_id, status, created_at, updated_at FROM offer_friendships WHERE from_user_id = ? AND to_user_id = ? FOR UPDATE` — `FOR UPDATE` блокирует найденную строку до конца транзакции сервиса, чтобы параллельный `accept`/`reject` не поменял статус между проверкой и ответом;
   - использовать существующий `ENTITY_MAPPER` и `jdbcTemplate.query(...)` + `stream().findFirst()` (не `queryForObject`, он бросает исключение на пустом результате).
3. В `OfferFriendshipPersistenceMapper` добавить `FindOfferFriendshipResponse toFindResponse(OfferFriendshipEntity entity)` (enum ↔ `String` MapStruct сделает сам).
4. Тесты в `FriendshipOfferRepositoryJdbcTest`:
   - заявка не существует → `Optional.empty()`;
   - заявка существует → возвращаются сохранённые данные, включая статус;
   - есть только встречная заявка (Борис → Алиса) → `Optional.empty()`;
   - статус `ACCEPTED`/`REJECTED` (выставить через `jdbcTemplate.update`) читается корректно.
5. Тест маппера в `OfferFriendshipPersistenceMapperTest` для `toFindResponse`, в том числе на неизвестный статус → `IllegalArgumentException`.

## Критерии приёмки

- `./mvnw test -Dtest=OfferFriendshipRepositoryJdbcTest,OfferFriendshipPersistenceMapperTest` зелёный.
- Сборка без предупреждений MapStruct об unmapped properties.
