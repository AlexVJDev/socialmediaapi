# Задание 07. Отписка: `SubscriptionRepository.deleteSubscription`

**Пункт задания:** нет отдельного, но без этого нельзя выполнить пункт 8 («исчезновение после отписки»).
**Зависит от:** ничего (можно делать параллельно с 03–06).

## Цель

Дать порту подписок операцию отписки, чтобы тест ленты проверял отписку через бизнес-контракт, а не через `DELETE` в `JdbcTemplate`.

## Контекст

- Сейчас в `SubscriptionRepository` есть только создание (`createSubscription`, `createSubscriptionIfAbsent`) и `existsSubscription`.
- Подписка удаляется физически: в `subscriptions` нет статуса, а уникальный ключ `uq_subscriptions_users` должен позволить подписаться снова.
- **Без check-then-act**: не проверять `existsSubscription` перед удалением. `DELETE ... WHERE` атомарен; результат — число удалённых строк.

## Что сделать

1. В `SubscriptionRepository`:
   ```java
   /**
    * Removes the subscription of the follower to the followed user.
    *
    * @return {@code true} if the subscription existed and was removed,
    *         {@code false} if there was nothing to remove
    */
   boolean deleteSubscription(UUID followerId, UUID followedId);
   ```
2. В `SubscriptionRepositoryJdbc`:
   ```sql
   DELETE FROM subscriptions WHERE follower_id = ? AND followed_id = ?
   ```
   `jdbcTemplate.update(...) == 1`.
3. Тесты в `SubscriptionRepositoryJdbcTest`:
   - существующая подписка → `true`, строки нет;
   - подписки нет → `false`;
   - удаляется только направление follower → followed: встречная подписка остаётся;
   - после отписки можно подписаться снова (`createSubscription` не падает на уникальном ключе).
4. Попутно (по замечанию с ревью про check-then-act): `existsSubscription` никем не вызывается — решить, удалять ли его вместе с тестами. Если оставляем — в Javadoc предупредить, что его нельзя использовать как проверку перед записью.

## Критерии приёмки

- `./mvnw test -Dtest=SubscriptionRepositoryJdbcTest` зелёный.
