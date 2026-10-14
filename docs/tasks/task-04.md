# Задание 04. Входной порт и сервис создания заявки — здесь бросаются ошибки

**Зависит от:** задания 03.

## Цель

Реализовать бизнес-правила ветки «1. Создание заявки» из `friendship_transitions.puml`: сервис проверяет предусловия и бросает доменные исключения из задания 02.

## Контекст

Слои по `CLAUDE.md`: `application.port.in` — входные порты (use case + Command/Result), реализация — сервис в `application`. Сервис зависит только от портов и домена, не от адаптеров и не от `*Entity`.
Время и идентификаторы для тестируемости получать извне: `java.time.Clock` (объявить бин в конфигурации, например `Clock.systemUTC()`) и генерация `UUID.randomUUID()` в сервисе.

## Что сделать

1. `application.port.in.CreateFriendshipOfferUseCase`:
   ```java
   CreateFriendshipOfferResult create(CreateFriendshipOfferCommand command);

   record CreateFriendshipOfferCommand(UUID fromUserId, UUID toUserId) { }
   ```
   Результат должен различать два успешных исхода — `201` (создана) и `204` (уже `PENDING`). Например:
   ```java
   sealed interface CreateFriendshipOfferResult {
       record Created(UUID id, UUID fromUserId, UUID toUserId,
                      FriendshipOfferStatus status, Instant createdAt) implements CreateFriendshipOfferResult { }
       record AlreadyPending() implements CreateFriendshipOfferResult { }
   }
   ```
2. `application.FriendshipOfferService implements CreateFriendshipOfferUseCase`, метод `create` помечен `@Transactional`. Алгоритм строго по схеме:
   1. `fromUserId.equals(toUserId)` → `throw new FriendshipOfferToSelfException(fromUserId)` (в БД не ходим);
   2. `repository.findByUsers(from, to)`:
      - пусто → `INSERT` заявки со статусом `PENDING` (`createFriendshipOffer`) и подписки Алиса → Борис, **если её ещё нет** → `Created`;
      - `PENDING` → `AlreadyPending`, ничего не меняется;
      - `ACCEPTED` → `throw new FriendshipOfferAlreadyAcceptedException(...)`;
      - `REJECTED` → `throw new FriendshipOfferAlreadyRejectedException(...)`.
   Ветвление по статусу — через `switch` по enum без `default`, чтобы компилятор подсказал при появлении нового статуса.
3. «Подписка, если её ещё нет»: в PostgreSQL ошибка внутри транзакции её прерывает, поэтому **нельзя** ловить `SubscriptionAlreadyExistsException` и продолжать. Добавить в `SubscriptionRepository` метод `boolean existsSubscription(UUID followerId, UUID followedId)` (реализация `SELECT EXISTS(...)` в `SubscriptionRepositoryJdbc` + тест) и вызывать `createSubscription` только если подписки нет.
4. Маппинг `CreateFriendshipOfferResponse` (порт out) → `Created` (порт in) — MapStruct-маппер в `application.mapper` (`FriendshipOfferApplicationMapper`, метод `toResult`), не вручную.
5. Юнит-тест `FriendshipOfferServiceTest` (Mockito, без Spring и БД), по тесту на ветку:
   - `from = to` → `FriendshipOfferToSelfException`, репозитории не вызывались;
   - заявки нет → `Created`, вызваны `createFriendshipOffer` и `createSubscription`;
   - заявки нет, подписка уже есть → `Created`, `createSubscription` не вызван;
   - `PENDING` → `AlreadyPending`, ничего не создаётся;
   - `ACCEPTED` → `FriendshipOfferAlreadyAcceptedException`, ничего не создаётся;
   - `REJECTED` → `FriendshipOfferAlreadyRejectedException`, ничего не создаётся.

## Критерии приёмки

- `./mvnw test -Dtest=FriendshipOfferServiceTest` зелёный.
- Сервис не импортирует классы из `adapter`.
- Проверка пользователей (`404 USER_NOT_FOUND`) в это задание не входит: таблицы пользователей в схеме БД нет.
