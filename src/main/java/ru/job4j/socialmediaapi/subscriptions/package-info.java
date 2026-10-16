/**
 * Подписки читателя на автора: создание (в том числе без ошибки на повтор) и отписка.
 * Данные — таблица {@code subscriptions}; другие модули обращаются к модулю через
 * {@link ru.job4j.socialmediaapi.subscriptions.api.Subscriptions}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Subscriptions",
        allowedDependencies = {}
)
package ru.job4j.socialmediaapi.subscriptions;
