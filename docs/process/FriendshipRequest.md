# Таблица переходов `FriendshipOffer`

Этот документ описывает переходы заявки в друзья для команд `friendshipOffer`, `acceptFriendship` и `rejectFriendship`.

## Переходы состояний

| Команда | Исходное состояние | Новое состояние | Изменение подписки | Изменение дружбы | Результат повторного вызова |
| --- | --- | --- | --- | --- | --- |
| `friendshipOffer` | Заявка не существует | `PENDING` | Инициатор подписывается на получателя | Без изменений | `409 Conflict`, `FriendshipOfferAlreadyPendingException`, состояние не изменяется |
| `acceptFriendship` | `PENDING` | `ACCEPTED` | Получатель подписывается на инициатора, если обратной подписки ещё нет | Между инициатором и получателем создаётся двусторонняя дружба | `204 No Content`, состояние не изменяется |
| `rejectFriendship` | `PENDING` | `REJECTED` | Без изменений: инициатор остаётся подписанным на получателя | Без изменений: дружба не создаётся | `204 No Content`, состояние не изменяется |
| `friendshipOffer` | `PENDING` | `PENDING` | Без изменений: инициатор уже подписан на получателя | Без изменений | `409 Conflict`, `FriendshipOfferAlreadyPendingException`, состояние не изменяется |
| `friendshipOffer` | `ACCEPTED` | `ACCEPTED` | Без изменений: принятую заявку нельзя вернуть в состояние `PENDING` | Без изменений: дружба уже существует | `409 Conflict`, `FriendshipOfferAlreadyAcceptedException`, состояние не изменяется |
| `friendshipOffer` | `REJECTED` | `REJECTED` | Без изменений: отклонённую заявку нельзя вернуть в состояние `PENDING` | Без изменений | `409 Conflict`, `FriendshipOfferAlreadyRejectedException`, состояние не изменяется |
| `acceptFriendship` | `ACCEPTED` | `ACCEPTED` | Без изменений: обратная подписка уже существует или должна была быть создана | Без изменений: дружба уже существует | `204 No Content`, состояние не изменяется |
| `rejectFriendship` | `ACCEPTED` | `ACCEPTED` | Без изменений | Без изменений: уже созданная дружба не отменяется командой отклонения | `409 Conflict`, `RejectFriendshipAlreadyAcceptedException`, состояние не изменяется |
| `acceptFriendship` | `REJECTED` | `REJECTED` | Без изменений | Без изменений: отклонённая заявка при повторном принятии не создаёт дружбу | `409 Conflict`, `AcceptedFriendshipAlreadyreRejectedException`, состояние не изменяется |
| `rejectFriendship` | `REJECTED` | `REJECTED` | Без изменений | Без изменений: дружба не была создана | `204 No Content`, состояние не изменяется |

## Правила идемпотентности

- Повторный вызов команды не должен создавать дубликаты заявок, подписок или связей дружбы.
- Для `acceptFriendship` и `rejectFriendship`: если целевое состояние уже достигнуто, система возвращает `204 No Content` и не изменяет данные.
- Повторный вызов `friendshipOffer` не создаёт вторую заявку (уникальность обеспечивает уникальный ключ БД) и возвращает `409 Conflict` с исключением по статусу существующей заявки:
  - `PENDING` — `FriendshipOfferAlreadyPendingException` (`FRIENDSHIP_OFFER_ALREADY_PENDING`), потому что заявка уже ожидает решения;
  - `ACCEPTED` — `FriendshipOfferAlreadyAcceptedException` (`FRIENDSHIP_OFFER_ALREADY_ACCEPTED`), потому что принятую заявку нельзя вернуть в состояние `PENDING`;
  - `REJECTED` — `FriendshipOfferAlreadyRejectedException` (`FRIENDSHIP_OFFER_ALREADY_REJECTED`), потому что отклонённую заявку нельзя вернуть в состояние `PENDING`.
- Отклонение заявки не отменяет подписку инициатора на получателя.
- Принятие заявки атомарно создаёт дружбу и обратную подписку.

## Ошибочные случаи

Для каждого ошибочного случая используется отдельный тип исключения. Это явно сообщает причину ошибки и позволяет внешнему слою выбрать подходящий HTTP-ответ.

| Случай | Команда | Исходное состояние | Новое состояние | Изменение подписки | Изменение дружбы | Исключение |
| --- | --- | --- | --- | --- | --- | --- |
| Пользователь предлагает дружбу самому себе | `friendshipOffer` | Заявка не существует | Без изменений | Без изменений | Без изменений | `FriendshipOfferToSelfException` |
| Заявка для принятия не найдена | `acceptFriendship` | Заявка не существует | Без изменений | Без изменений | Без изменений | `FriendshipRequestNotFoundException` |
| Заявка для отклонения не найдена | `rejectFriendship` | Заявка не существует | Без изменений | Без изменений | Без изменений | `FriendshipRequestNotFoundException` |
| Повторное предложение, заявка ждёт решения | `friendshipOffer` | `PENDING` | `PENDING` | Без изменений | Без изменений | `FriendshipOfferAlreadyPendingException` |
| Повторное предложение после принятия | `friendshipOffer` | `ACCEPTED` | `ACCEPTED` | Без изменений | Без изменений | `FriendshipOfferAlreadyAcceptedException` |
| Повторное предложение после отклонения | `friendshipOffer` | `REJECTED` | `REJECTED` | Без изменений | Без изменений | `FriendshipOfferAlreadyRejectedException` |

## Повторное предложение дружбы после отклонения

После перехода в состояние `REJECTED` повторный вызов `friendshipOffer` не создаёт новую заявку и завершается конфликтом.

Если продукт должен разрешать повторное предложение через некоторое время, потребуется отдельное бизнес-правило. Такое правило должно явно описывать, когда отклонённую заявку можно заменить новой, например после окончания периода ожидания.
