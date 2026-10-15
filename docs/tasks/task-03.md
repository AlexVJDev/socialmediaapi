   # Задание 03. Поиск существующей заявки в выходном порте

**Зависит от:** задания 02.

## Цель

Дать сервису возможность узнать, есть ли уже заявка в направлении Алиса → Борис и в каком она статусе — без этого нельзя различить «создать», «повтор `PENDING`» и ошибки `ACCEPTED`/`REJECTED`.

## Контекст

Схема: `Api -> DB : найти заявку Алиса -> Борис`. Проверка относится только к этому направлению (встречная заявка Борис → Алиса — отдельная строка, `uq_friendship_offer_users` это допускает).
Слой порта: `application.port.out.FriendshipOfferRepository` (Request/Response — вложенные records). Адаптер: `FriendshipOfferRepositoryJdbc` (SQL → Row через `RowMapper` → Response через MapStruct-маппер). Индекс `uq_friendship_offer_users (from_user_id, to_user_id)` уже покрывает поиск.

## Что сделать

1. В `FriendshipOfferRepository` добавить:
   ```java
   Optional<FindFriendshipOfferResponse> findByUsers(UUID fromUserId, UUID toUserId);

   record FindFriendshipOfferResponse(
           UUID id, UUID fromUserId, UUID toUserId,
           FriendshipOfferStatus status, Instant createdAt, Instant updatedAt
   ) { }
   ```
   (Если структура совпадёт с `CreateFriendshipOfferResponse` один в один — допустимо переиспользовать его вместо нового record; решение зафиксировать в Javadoc.)
2. В `FriendshipOfferRepositoryJdbc` реализовать метод:
   - SQL: `SELECT id, from_user_id, to_user_id, status, created_at, updated_at FROM friendship_offer WHERE from_user_id = ? AND to_user_id = ? FOR UPDATE` — `FOR UPDATE` блокирует найденную строку до конца транзакции сервиса, чтобы параллельный `accept`/`reject` не поменял статус между проверкой и ответом;
   - использовать существующий `ROW_MAPPER` и `jdbcTemplate.query(...)` + `stream().findFirst()` (не `queryForObject`, он бросает исключение на пустом результате).
3. В `FriendshipOfferPersistenceMapper` добавить `FindFriendshipOfferResponse toFindResponse(FriendshipOfferRow row)` (enum ↔ `String` MapStruct сделает сам).
4. Тесты в `FriendshipOfferRepositoryJdbcTest`:
   - заявка не существует → `Optional.empty()`;
   - заявка существует → возвращаются сохранённые данные, включая статус;
   - есть только встречная заявка (Борис → Алиса) → `Optional.empty()`;
   - статус `ACCEPTED`/`REJECTED` (выставить через `jdbcTemplate.update`) читается корректно.
5. Тест маппера в `FriendshipOfferPersistenceMapperTest` для `toFindResponse`, в том числе на неизвестный статус → `IllegalArgumentException`.

## Критерии приёмки

- `./mvnw test -Dtest=FriendshipOfferRepositoryJdbcTest,FriendshipOfferPersistenceMapperTest` зелёный.
- Сборка без предупреждений MapStruct об unmapped properties.
