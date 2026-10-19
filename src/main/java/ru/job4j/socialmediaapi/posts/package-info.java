/**
 * Посты (создание, изменение автором, логическое удаление) и лента подписчика.
 * Данные — таблица {@code posts}; лента строится на чтении одним запросом
 * {@code subscriptions JOIN posts} — осознанное исключение из правила
 * «не читать чужие таблицы» (см. {@code docs/architecture.md}). В Java-коде
 * модуль от других модулей не зависит; публичного {@code api} пока нет.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Posts",
        allowedDependencies = {}
)
package ru.job4j.socialmediaapi.posts;
