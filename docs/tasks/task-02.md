# Задание 02. Доменные исключения для ошибок создания заявки

**Зависит от:** задания 01.

## Цель

Завести по отдельному типу исключения на каждую ошибку из схемы (правило из `FriendshipRequest.md`: «Для каждого ошибочного случая используется отдельный тип исключения»).

## Контекст

Исключения домена лежат в `ru.job4j.socialmediaapi.domain.friendshipoffer.exception`, наследуют `RuntimeException`, сообщение — на английском через `"...".formatted(...)`.
`FriendshipOfferToSelfException` бросает домен (фабрика `FriendshipOffer.create`) **до** обращения к БД. JDBC-адаптер его не бросает: срабатывание `chk_friendship_offer_different_users` означает ошибку в коде и пробрасывается как `DataIntegrityViolationException` (см. `task_new.md`).

## Что сделать

1. `FriendshipOfferToSelfException` — единственный конструктор `(UUID userId)` без `cause`.
2. Создать `FriendshipOfferAlreadyAcceptedException`:
   - конструктор `(UUID friendshipOfferId, UUID fromUserId, UUID toUserId)`;
   - сообщение вида `Offer friendship %s from user %s to user %s is already accepted`.
3. Создать `FriendshipOfferAlreadyRejectedException` — аналогично, `... is already rejected`.
4. Исключения не знают об HTTP и кодах ошибок: соответствие «исключение → HTTP-статус и `code`» задаётся в веб-адаптере (задание 06).

## Критерии приёмки

- `./mvnw clean compile` проходит.
- Пакет `domain` по-прежнему не импортирует ничего из `org.springframework`, `adapter` и `application`.
