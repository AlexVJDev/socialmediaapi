# 4123 — журнал решений (Тестирование репозитория с Testcontainers)

Общие решения задачи 412 — `../4121/decisions.md`; JPA — `../4122/decisions.md`.

| ID | Решение (1–2 строки) | Источник | Статус |
|---|---|---|---|
| Р3-1 | API создания заявки — наш (`/api/v1/friendship-offer/create`, `X-User-Id`, PENDING → 200, ACCEPTED/REJECTED → 409, себе → 400); сервис и контроллер урока не добавляем. | fix.md в.1, 24.11.2026 | действует |
| Р3-2 | Один PostgreSQL-контейнер на весь прогон: статический singleton в `TestcontainersConfiguration`, `@Bean(destroyMethod = "")`; `FriendshipOfferPersistenceAdapterTest` — на `@Import(TestcontainersConfiguration)` вместо своего `@Container`. Базовый класс `PostgreSqlIntegrationTest` не вводим. | fix.md в.2, 24.11.2026 | действует |
| Р3-3 | `FriendshipOfferControllerTest`: заявки читаются через `FriendshipOfferJpaRepository`, после повтора заявка сравнивается целиком (`usingRecursiveComparison`); подписки и дружбы — `JdbcTemplate`. | fix.md в.3, 24.11.2026 | действует |
| Р3-4 | Тестовый код 4123 пишет Claude по плану; запускает пользователь (Р2-12). | fix.md в.4, 24.11.2026 | действует |
