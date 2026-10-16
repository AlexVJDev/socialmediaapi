/**
 * Заявки в друзья и дружба: создание заявки (с подпиской отправителя на получателя),
 * в будущем — принятие и отклонение. Данные — таблицы {@code friendship_offer}
 * и {@code friendships}. Подписки создаются через публичный API модуля {@code subscriptions}
 * в той же транзакции.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Friendships",
        allowedDependencies = {"subscriptions::api"}
)
package ru.job4j.socialmediaapi.friendships;
