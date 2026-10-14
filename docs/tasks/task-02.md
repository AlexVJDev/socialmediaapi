# Задание 02. Доменные исключения для ошибок создания заявки

**Зависит от:** задания 01.

## Цель

Завести по отдельному типу исключения на каждую ошибку из схемы (правило из `FriendshipRequest.md`: «Для каждого ошибочного случая используется отдельный тип исключения»).

## Контекст

Исключения домена лежат в `ru.job4j.socialmediaapi.domain.friendshipoffer.exception`, наследуют `RuntimeException`, сообщение — на английском через `"...".formatted(...)`.
`FriendshipOfferToSelfException` уже есть, но у него только конструктор с `cause`: его бросает JDBC-адаптер при нарушении `chk_friendship_offer_different_users`. Сервис (задание 04) должен бросать его **до** обращения к БД, без причины.

## Что сделать

1. `FriendshipOfferToSelfException` — добавить конструктор `(UUID userId)` без `cause`. Существующий конструктор оставить (им пользуется адаптер).
2. Создать `FriendshipOfferAlreadyAcceptedException`:
   - конструктор `(UUID friendshipOfferId, UUID fromUserId, UUID toUserId)`;
   - сообщение вида `Offer friendship %s from user %s to user %s is already accepted`.
3. Создать `FriendshipOfferAlreadyRejectedException` — аналогично, `... is already rejected`.
4. Исключения не знают об HTTP и кодах ошибок: соответствие «исключение → HTTP-статус и `code`» задаётся в веб-адаптере (задание 06).

## Критерии приёмки

- `./mvnw clean compile` проходит.
- Пакет `domain` по-прежнему не импортирует ничего из `org.springframework`, `adapter` и `application`.
