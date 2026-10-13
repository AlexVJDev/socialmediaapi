# Задание 04. Входной порт и сервис создания заявки — здесь бросаются ошибки

**Зависит от:** задания 03.

## Цель

Реализовать бизнес-правила ветки «1. Создание заявки» из `friendship_transitions.puml`: сервис проверяет предусловия и бросает доменные исключения из задания 02.

## Контекст

Слои по `CLAUDE.md`: `application.port.in` — входные порты (use case + Command/Result), реализация — сервис в `application`. Сервис зависит только от портов и домена, не от адаптеров и не от `*Entity`.
Время и идентификаторы для тестируемости получать извне: `java.time.Clock` (объявить бин в конфигурации, например `Clock.systemUTC()`) и генерация `UUID.randomUUID()` в сервисе.

## Что сделать

1. `application.port.in.CreateOfferFriendshipUseCase`:
   ```java
   CreateOfferFriendshipResult create(CreateOfferFriendshipCommand command);

   record CreateOfferFriendshipCommand(UUID fromUserId, UUID toUserId) { }
   ```
   Результат должен различать два успешных исхода — `201` (создана) и `204` (уже `PENDING`). Например:
   ```java
   sealed interface CreateOfferFriendshipResult {
       record Created(UUID id, UUID fromUserId, UUID toUserId,
                      OfferFriendshipStatus status, Instant createdAt) implements CreateOfferFriendshipResult { }
       record AlreadyPending() implements CreateOfferFriendshipResult { }
   }
   ```
2. `application.OfferFriendshipService implements CreateOfferFriendshipUseCase`, метод `create` помечен `@Transactional`. Алгоритм строго по схеме:
   1. `fromUserId.equals(toUserId)` → `throw new OfferFriendshipToSelfException(fromUserId)` (в БД не ходим);
   2. `repository.findByUsers(from, to)`:
      - пусто → `INSERT` заявки со статусом `PENDING` (`createOfferFriendship`) и подписки Алиса → Борис, **если её ещё нет** → `Created`;
      - `PENDING` → `AlreadyPending`, ничего не меняется;
      - `ACCEPTED` → `throw new OfferFriendshipAlreadyAcceptedException(...)`;
      - `REJECTED` → `throw new OfferFriendshipAlreadyRejectedException(...)`.
   Ветвление по статусу — через `switch` по enum без `default`, чтобы компилятор подсказал при появлении нового статуса.
3. «Подписка, если её ещё нет»: в PostgreSQL ошибка внутри транзакции её прерывает, поэтому **нельзя** ловить `SubscriptionAlreadyExistsException` и продолжать. Добавить в `SubscriptionRepository` метод `boolean existsSubscription(UUID followerId, UUID followedId)` (реализация `SELECT EXISTS(...)` в `SubscriptionRepositoryJdbc` + тест) и вызывать `createSubscription` только если подписки нет.
4. Маппинг `CreateOfferFriendshipResponse` (порт out) → `Created` (порт in) — MapStruct-маппер в `application.mapper` (`OfferFriendshipApplicationMapper`, метод `toResult`), не вручную.
5. Юнит-тест `OfferFriendshipServiceTest` (Mockito, без Spring и БД), по тесту на ветку:
   - `from = to` → `OfferFriendshipToSelfException`, репозитории не вызывались;
   - заявки нет → `Created`, вызваны `createOfferFriendship` и `createSubscription`;
   - заявки нет, подписка уже есть → `Created`, `createSubscription` не вызван;
   - `PENDING` → `AlreadyPending`, ничего не создаётся;
   - `ACCEPTED` → `OfferFriendshipAlreadyAcceptedException`, ничего не создаётся;
   - `REJECTED` → `OfferFriendshipAlreadyRejectedException`, ничего не создаётся.

## Критерии приёмки

- `./mvnw test -Dtest=OfferFriendshipServiceTest` зелёный.
- Сервис не импортирует классы из `adapter`.
- Проверка пользователей (`404 USER_NOT_FOUND`) в это задание не входит: таблицы пользователей в схеме БД нет.
