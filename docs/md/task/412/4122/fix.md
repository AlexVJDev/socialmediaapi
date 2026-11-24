Режим Ментора: ДА

Общие решения задачи 412 — `../4121/decisions.md` Р-1…Р-3. Анализ — `notes.md`.

## Раунд 2
Ответ перенесён в decisions.md Р2-13.

## Раунд 1
Ответы перенесены в decisions.md Р2-1…Р2-10.

## К сведению
- `FriendshipOfferRow` и `RowMapper` адаптера заявок удаляются; `FriendshipOfferPersistenceMapper` будет маппить сущность в модели порта.
- `PostgresErrors` в модуле friendships остаётся — им пользуется `FriendshipsPersistenceAdapter` (JDBC).
- В уроке тест вставки сравнивает сущности через `usingRecursiveComparison()` — так и сделаем.
