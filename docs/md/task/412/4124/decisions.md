# 4124 — журнал решений (Запросы подписчиков и друзей)

Общие решения — `../4121/decisions.md`; JPA — `../4122/decisions.md`; тесты — `../4123/decisions.md`.

| ID | Решение (1–2 строки) | Источник | Статус |
|---|---|---|---|
| Р4-1 | На JPA переводим `friendships` и `subscriptions` целиком (все методы портов `FriendshipsRepository`, `SubscriptionRepository`); порты и сценарии не меняются. JDBC остаётся только в posts. | fix.md в.1, 24.11.2026 | действует |
| Р4-2 | По образцу 4122: `FriendshipEntity` + `FriendshipJpaRepository` (`friendships/…/persistence/jpa/`), `SubscriptionEntity` + `SubscriptionJpaRepository` (`subscriptions/…/persistence/jpa/`); `Repository<…, UUID>`, без `@GeneratedValue`, транзакции как Р2-11; статус — доменный `FriendshipStatus`. | fix.md в.2, 24.11.2026 | действует |
| Р4-3 | `friendship_offer_id` в `FriendshipEntity` — поле `UUID`, без `@ManyToOne`. | fix.md в.3, 24.11.2026 | действует |
| Р4-4 | Друзья — курсор `(created_at, id)` и нативный `UNION ALL` двух веток (индексы 011) с проекцией; `Slice`/`Pageable` урока не используем. | fix.md в.4, 24.11.2026 | действует |
| Р4-5 | Подписки/подписчики — по-прежнему весь список пользователя (`List`); страницы — отдельная задача (кандидат в `notes.md`). | fix.md в.5, 24.11.2026 | действует |
| Р4-6 | `createFriendship`, `createSubscription` — native `INSERT … ON CONFLICT DO NOTHING`, 0 → прежнее исключение; `createSubscriptionIfAbsent` — 0 → пусто, 1 → чтение по id. `PostgresErrors` в friendships и subscriptions удаляем. | fix.md в.6, 24.11.2026 | действует |
| Р4-7 | `FriendshipsRow`, `FriendRow`, `SubscriptionRow`, `RowMapper` удаляем; строка страницы друзей — проекция `FriendView`; мапперы — на сущностях, их тесты переделываем. | fix.md в.7, 24.11.2026 | действует |
| Р4-8 | Лог SQL: `logging.level.org.hibernate.SQL: ${SQL_LOG_LEVEL:info}`; локально `SQL_LOG_LEVEL=debug`. | fix.md в.8, 24.11.2026 | действует |
| Р4-9 | Тесты (пишет Claude, запускает пользователь): `FriendshipJpaRepositoryTest` (друг с любой стороны пары), `SubscriptionJpaRepositoryTest` (направление подписки); тесты адаптеров через порт не меняем. | fix.md в.9, 24.11.2026 | действует |
