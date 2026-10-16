/**
 * Служебные эндпоинты приложения, не относящиеся к бизнес-областям:
 * {@code GET /api/ping} — проверка, что сервис запущен. Данных и зависимостей нет.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "System",
        allowedDependencies = {}
)
package ru.job4j.socialmediaapi.system;
