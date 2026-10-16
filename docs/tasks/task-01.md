# Задание 01. Согласовать контракт создания заявки со схемой

## Цель

Привести OpenAPI-контракт `POST /api/offer-friendships/create` в соответствие со схемой `docs/process/friendship_transitions.puml`, чтобы код дальнейших заданий опирался на один набор кодов ошибок.

## Контекст

Схема описывает три ошибки создания заявки:

| Ситуация | HTTP | `code` |
| --- | --- | --- |
| Алиса = Борис (заявка самому себе) | `400` | `INVALID_FRIENDSHIP_OFFER_REQUEST` |
| Повтор: заявка уже `ACCEPTED` | `409` | `FRIENDSHIP_OFFER_ALREADY_ACCEPTED` |
| Повтор: заявка уже `REJECTED` | `409` | `FRIENDSHIP_OFFER_ALREADY_REJECTED` |

Сейчас в документации есть расхождения:

- `docs/api/paths/friendship-offer-{create,accept,reject}.yaml` используют коды `OFFER_FRIENDSHIP_ALREADY_ACCEPTED` / `OFFER_FRIENDSHIP_ALREADY_REJECTED`, а схема — `FRIENDSHIP_OFFER_ALREADY_*` (как и уже существующий `FRIENDSHIP_OFFER_NOT_FOUND`);
- идентификаторы в контракте — `integer/int64`, в БД — `UUID`;
- в `docs/process/FriendshipRequest.md` в «Правилах идемпотентности» повтор `friendshipOffer` для `ACCEPTED` даёт `204`, а в таблице переходов и в схеме — `409`.

## Что сделать

1. В `friendship-offer-create.yaml`, `friendship-offer-accept.yaml`, `friendship-offer-reject.yaml` заменить `OFFER_FRIENDSHIP_ALREADY_ACCEPTED` → `FRIENDSHIP_OFFER_ALREADY_ACCEPTED` и `OFFER_FRIENDSHIP_ALREADY_REJECTED` → `FRIENDSHIP_OFFER_ALREADY_REJECTED`.
2. В `friendship-offer-create.yaml` для ответа `400` уточнить `description`: ошибка возникает и при некорректном теле (нет `fromUserId`/`toUserId`, неверный формат), и при `fromUserId = toUserId`.
3. Перевести `fromUserId`, `toUserId`, `id` в запросе и ответе create на `type: string, format: uuid` (примеры — UUID), как в БД.
4. Обновить `docs/api/friendship-offer-create.md`: типы полей (`string (uuid)`), примеры, таблицу ошибок — с кодами из таблицы выше.
5. Исправить строку про `ACCEPTED` в «Правилах идемпотентности» `FriendshipRequest.md`: повтор → `409 Conflict`, `FriendshipOfferAlreadyAcceptedException`.
6. В `../CLAUDE2.md` заменить пример кода ошибки `OFFER_FRIENDSHIP_ALREADY_REJECTED` на `FRIENDSHIP_OFFER_ALREADY_REJECTED` и убрать из «Known inconsistencies» пункты, которые этим заданием устранены.

## Критерии приёмки

- В `docs/` нет вхождений `OFFER_FRIENDSHIP_ALREADY_` (проверка: `grep -rn OFFER_FRIENDSHIP_ALREADY_ docs`).
- Контракт валиден: `docker compose --profile tools run --rm openapi-validator`.
- Swagger UI (`docker compose up -d swagger-ui`, http://localhost:8081) показывает UUID-поля и новые коды для create.
