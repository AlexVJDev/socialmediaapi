# Задание 04. Входной порт и сервис создания заявки — здесь бросаются ошибки

**Зависит от:** задания 03.

## Цель

Реализовать бизнес-правила ветки «1. Создание заявки» из `friendship_transitions.puml`: сервис проверяет предусловия и бросает доменные исключения из задания 02.

## Контекст

Слои по `../../../../CLAUDE.md`: `application.port.in` — входные порты (use case + UseCaseRequest/Result), реализация — сервис в `application`. Сервис зависит только от портов и домена, не от адаптеров и не от `*Row`.
Время и идентификаторы для тестируемости получать извне: `java.time.Clock` (объявить бин в конфигурации, например `Clock.systemUTC()`) и генерация `UUID.randomUUID()` в сервисе.

## Что сделать

1. `application.port.in.CreateFriendshipOfferUseCase`:
   ```java
   CreateFriendshipOfferResult create(CreateFriendshipOfferUseCaseRequest useCaseRequest);

   record CreateFriendshipOfferUseCaseRequest(UUID fromUserId, UUID toUserId) { }
   ```
   Успешный исход у создания один — заявка создана (`201`), поэтому результат — обычный record:
   ```java
   record CreateFriendshipOfferResult(UUID id, UUID fromUserId, UUID toUserId,
                                      FriendshipOfferStatus status, Instant createdAt) { }
   ```
   Повтор — это ошибка, которую бросает сервис (`409`), а не второй вид результата.
2. `application.FriendshipOfferService implements CreateFriendshipOfferUseCase`, метод `create` помечен `@Transactional`. Алгоритм строго по схеме:
   1. `fromUserId.equals(toUserId)` → `throw new FriendshipOfferToSelfException(fromUserId)` (в БД не ходим);
   2. сразу `INSERT` заявки со статусом `PENDING` (`createFriendshipOffer`), **без предварительного поиска**. Уникальность заявки на пару пользователей — инвариант, который обеспечивает уникальный ключ `uq_friendship_offer_users`; схема «найдём, если нет — создадим» дублирует его в коде и оставляет окно для гонки между поиском и вставкой:
      - вставка прошла → подписка Алиса → Борис, **если её ещё нет** → результат с данными созданной заявки;
      - `FriendshipOfferAlreadyExistsException` (нарушен уникальный ключ — это повтор) → `repository.findByUsers(from, to)` и исключение по статусу найденной заявки:
        - `PENDING` → `throw new FriendshipOfferAlreadyPendingException(...)` (из задания 02);
        - `ACCEPTED` → `throw new FriendshipOfferAlreadyAcceptedException(...)`;
        - `REJECTED` → `throw new FriendshipOfferAlreadyRejectedException(...)`;
        - заявка не найдена (такого быть не должно) → пробросить исходное `FriendshipOfferAlreadyExistsException`, отдельную проверку через `orElseThrow` не писать.

      Всё это — один private-метод, который возвращает исключение для `throw`, а не бросает его сам:
      ```java
      /**
      } catch (FriendshipOfferAlreadyExistsException e) {
          throw alreadyExistsError(offer, e);
      }

      private RuntimeException alreadyExistsError(FriendshipOffer offer, FriendshipOfferAlreadyExistsException cause) {
          return friendshipOfferRepository.findByUsers(offer.fromUserId(), offer.toUserId())
                  .<RuntimeException>map(existing -> switch (existing.status()) {
                      case PENDING -> new FriendshipOfferAlreadyPendingException(...);
                      case ACCEPTED -> new FriendshipOfferAlreadyAcceptedException(...);
                      case REJECTED -> new FriendshipOfferAlreadyRejectedException(...);
                  })
                  .orElse(cause);
      } */
      ```
      Явный тип `.<RuntimeException>map(...)` нужен, чтобы `.orElse(cause)` скомпилировался.

      PostgreSQL прерывает транзакцию после нарушения уникального ключа, поэтому `createFriendshipOffer` в адаптере помечается `@Transactional(propagation = NESTED)`: savepoint откатывает только `INSERT`, и в той же транзакции заявку можно перечитать (подробнее — задание 07). После `throw` транзакция сервиса откатывается целиком — ничего не меняется.
   Ветвление по статусу — через `switch` по enum без `default`, чтобы компилятор подсказал при появлении нового статуса.
3. «Подписка, если её ещё нет»: в PostgreSQL ошибка внутри транзакции её прерывает, поэтому **нельзя** ловить `SubscriptionAlreadyExistsException` и продолжать. Проверка «сначала `existsSubscription`, потом `createSubscription`» тоже не подходит: между проверкой и вставкой подписку может создать параллельный запрос (например, принятие встречной заявки), и `INSERT` упадёт. Добавить в `SubscriptionRepository` метод `Optional<CreateSubscriptionResponse> createSubscriptionIfAbsent(CreateSubscriptionRequest request)`: реализация в `SubscriptionRepositoryJdbc` — `INSERT ... ON CONFLICT (follower_id, followed_id) DO NOTHING RETURNING ...`, пустой результат означает, что подписка уже была (+ тест). Сервис вызывает только его: дубликат подписки — не ошибка, транзакция не прерывается.
4. Маппинг `CreateFriendshipOfferResponse` (порт out) → `CreateFriendshipOfferResult` (порт in) — MapStruct-маппер в `application.mapper` (`FriendshipOfferApplicationMapper`, метод `toResult`), не вручную.
5. Юнит-тест `FriendshipOfferServiceTest` (Mockito, без Spring и БД), по тесту на ветку:
   - `from = to` → `FriendshipOfferToSelfException`, репозитории не вызывались;
   - заявки нет → результат с данными заявки, вызваны `createFriendshipOffer` и `createSubscriptionIfAbsent`, `findByUsers` не вызывался;
   - заявки нет, подписка уже есть (`createSubscriptionIfAbsent` вернул пусто) → заявка создана, `createSubscription` не вызван;
   - `createFriendshipOffer` бросил `FriendshipOfferAlreadyExistsException`, найдена заявка (параметризованный тест, `@MethodSource` «статус → класс исключения»):
     - `PENDING` → `FriendshipOfferAlreadyPendingException`, подписка не создаётся;
     - `ACCEPTED` → `FriendshipOfferAlreadyAcceptedException`, подписка не создаётся;
     - `REJECTED` → `FriendshipOfferAlreadyRejectedException`, подписка не создаётся;
   - `createFriendshipOffer` бросил `FriendshipOfferAlreadyExistsException`, заявка не найдена → брошено то же самое исключение (`isSameAs`).

## Критерии приёмки

- `./mvnw test -Dtest=FriendshipOfferServiceTest` зелёный.
- Сервис не импортирует классы из `adapter`.
- Проверка пользователей (`404 USER_NOT_FOUND`) в это задание не входит: таблицы пользователей в схеме БД нет.
