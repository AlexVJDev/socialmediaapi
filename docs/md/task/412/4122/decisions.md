# 4122 — журнал решений (JPA-сущности и репозитории)

Общие решения задачи 412 (Р-1…Р-3, Р1-1…Р1-6) — `../4121/decisions.md`.

| ID | Решение (1–2 строки) | Источник | Статус |
|---|---|---|---|
| Р2-1 | На JPA переводим только `friendship_offer`; `friendships`, `subscriptions`, `posts` — на JDBC до 4124–4126. | fix.md в.1, 24.11.2026 | действует |
| Р2-2 | Адаптер `friendship_offer` переводим целиком (все 8 методов); порт `FriendshipOfferRepository` и сценарии не меняются. | fix.md в.2, 24.11.2026 | действует |
| Р2-3 | Порт `FriendshipOfferRepository` остаётся (контракт application). Новые классы: `FriendshipOfferEntity`, `FriendshipOfferJpaRepository` в `friendships/internal/infrastructure/persistence/jpa/`; конструктор со всеми полями — package-private. | fix.md в.3 + чат, 24.11.2026 | действует |
| Р2-4 | Статус в сущности — доменный `FriendshipOfferStatus`, новый enum не создаём. | fix.md в.4, 24.11.2026 | действует |
| Р2-5 | `FriendshipOfferJpaRepository` наследует `Repository<FriendshipOfferEntity, UUID>`, методы объявляются явно (нет `findAll`/`deleteAll`). | fix.md в.5, 24.11.2026 | действует |
| Р2-6 | `@GeneratedValue` не используем: id задаёт домен, вставка нативная с переданным id. | fix.md в.6, 24.11.2026 | действует |
| Р2-7 | Создание: нативный `INSERT … ON CONFLICT DO NOTHING` (без цели); 0 строк → `FriendshipOfferAlreadyExistsException`. `NESTED` и `PostgresErrors` в адаптере заявок убираем; Javadoc порта и `CreateFriendshipOffer` про savepoint правим. | fix.md в.7, 24.11.2026 | действует |
| Р2-8 | Запросы: производные `findById`, `findByIdAndToUserId`; пара — JPQL + `@Lock(PESSIMISTIC_WRITE)`; статус — `@Modifying` JPQL UPDATE + чтение по id; страницы — 4 нативных запроса с `(created_at, id) < (?, ?)`. | fix.md в.8, 24.11.2026 | действует |
| Р2-9 | `spring-boot-starter-jdbc` → `spring-boot-starter-data-jpa`; + `spring-boot-starter-data-jpa-test` (test); `spring.jpa.hibernate.ddl-auto: validate`, `spring.jpa.open-in-view: false`. | fix.md в.9, 24.11.2026 | действует |
| Р2-10 | `FriendshipOfferPersistenceAdapterTest` не меняем; новый `FriendshipOfferJpaRepositoryTest` (`@DataJpaTest` + `TestcontainersConfiguration`) — 2 сценария урока; `PostgreSqlIntegrationTest` — в 4123. | fix.md в.10, 24.11.2026 | действует |
| Р2-11 | `FriendshipOfferJpaRepository`: `@Transactional(readOnly = true)` на интерфейсе, `@Transactional` на `create`, `findByPairForUpdate`, `updateStatusIfPending` — JPA не выполняет `@Modifying` и блокировки вне транзакции (адаптер вызывается и без сценария). | прогон тестов, 24.11.2026 | действует |
| Р2-12 | Тесты (автоматические прогоны и ручные) делает пользователь; Claude их не запускает. Пункты 9–10 плана — за пользователем. | чат, 24.11.2026 | действует |
| Р2-13 | Persistence-адаптеры (friendships, posts, subscriptions) — `@Component` вместо `@Repository`: с JPA перевод исключений `@Repository` превращал `IllegalArgumentException` в `InvalidDataAccessApiUsageException`; JdbcTemplate и Spring Data переводят свои исключения сами. | fix.md р.2 в.1, 24.11.2026 | действует |
| Р2-14 | `FriendshipOfferPersistenceAdapterTest`: в тестах дубля и встречной заявки `hasCauseInstanceOf(DataIntegrityViolationException)` → `hasNoCause()` — после Р2-7 ошибки БД нет, исключение адаптера без причины. Уточняет Р2-10. | чат, 24.11.2026 (тесты упали) | действует |
