/**
 * Подписки читателя на автора: создание (в том числе без ошибки на повтор), отписка и чтение
 * списков подписок и подписчиков (REST {@code /api/v1/subscribes[/followers]}).
 * Данные — таблица {@code subscriptions}; другие модули обращаются к модулю через
 * {@link ru.job4j.socialmediaapi.subscriptions.api.Subscriptions}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Subscriptions",
        allowedDependencies = {}
)
package ru.job4j.socialmediaapi.subscriptions;
