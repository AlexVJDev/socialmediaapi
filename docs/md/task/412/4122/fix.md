Режим Ментора: ДА

Общие решения задачи 412 — `../4121/decisions.md` Р-1…Р-3. Анализ — `notes.md`.

## Требует ответа

**1. Объём 4122: на JPA переводим только `friendship_offer` (как в уроке); `friendships`, `subscriptions`, `posts` остаются на JDBC до 4124–4126?**
**Рекомендация:** да; JdbcTemplate и JPA работают в одной транзакции (`JpaTransactionManager` обслуживает обоих).
Ответ: Принимается

**2. Адаптер `friendship_offer`: перевести все 8 методов порта на JPA или только create и поиск пары, как в уроке?**
**Рекомендация:** все 8 — иначе в одном адаптере два способа доступа; порт `FriendshipOfferRepository` не меняется.
Ответ: Принимается

**3. Имена и пакет. В уроке `OfferFriendshipEntity`, `OfferFriendshipRepository` в `ru.job4j.socialmedia.repository`.**
У нас имя `FriendshipOfferRepository` занято портом. Вариант: `FriendshipOfferEntity` + `FriendshipOfferJpaRepository` в `friendships/internal/infrastructure/persistence/jpa/`.
**Рекомендация:** этот вариант (имена по таблице — Р-1; один пакет — конструктор сущности package-private, как в уроке).
Ответ: Принимается

**4. Статус: новый enum `OfferFriendshipStatus` (урок) или существующий доменный `FriendshipOfferStatus`?**
**Рекомендация:** доменный — значения те же, второй enum дублировал бы его.
Ответ: Принимается

**5. Базовый интерфейс: `JpaRepository` (урок) или `Repository<FriendshipOfferEntity, UUID>` с явными методами?**
`JpaRepository` открывает `findAll()`, `deleteAll()` — запросы по всей таблице, запрещённые правилами проекта.
**Рекомендация:** `Repository` и только нужные методы.
Ответ: Принимается

**6. `@GeneratedValue(strategy = UUID)` на `id` (урок)?**
У нас id создаёт домен (`FriendshipOffer.create`), вставка — нативным запросом с переданным id; JPA id не генерирует.
**Рекомендация:** без `@GeneratedValue`.
Ответ: Принимается

**7. Создание заявки: как в уроке — `INSERT … ON CONFLICT DO NOTHING`, вернул 0 → адаптер бросает `FriendshipOfferAlreadyExistsException`.**
Порт и сценарий `CreateFriendshipOffer` не меняются; savepoint (`Propagation.NESTED`) и разбор имени constraint (`PostgresErrors`) в адаптере заявок больше не нужны. Цель `ON CONFLICT` не указываем (Р1-4).
**Рекомендация:** принять; Javadoc порта и `CreateFriendshipOffer` про savepoint поправить.
Ответ: Принимается

**8. Запросы:** `findById`, `findByIdAndToUserId` — производные методы; поиск пары с блокировкой — JPQL + `@Lock(PESSIMISTIC_WRITE)`; смена статуса — `@Modifying` JPQL `UPDATE … WHERE status = PENDING` + чтение по id; страницы — нативный SQL (сравнение `(created_at, id) < (?, ?)` в JPQL не выражается, а индекс работает именно по нему).
**Рекомендация:** принять.
Ответ: Принимается

**9. Конфигурация:** `spring-boot-starter-jdbc` → `spring-boot-starter-data-jpa` (JdbcTemplate входит); тест — `spring-boot-starter-data-jpa-test`; `spring.jpa.hibernate.ddl-auto: validate` (схему создаёт только Liquibase, Hibernate сверяет); `spring.jpa.open-in-view: false`.
**Рекомендация:** принять.
Ответ: Принимается

**10. Тесты:** `FriendshipOfferPersistenceAdapterTest` (21 тест через порт) не меняем — проверка, что поведение сохранилось. Новый `FriendshipOfferJpaRepositoryTest` (`@DataJpaTest` + существующий `TestcontainersConfiguration`): два сценария урока с `@DisplayName`. Базовый класс `PostgreSqlIntegrationTest` — в 4123 (там урок про Testcontainers).
**Рекомендация:** принять.
Ответ: Принимается

## К сведению
- `FriendshipOfferRow` и `RowMapper` адаптера заявок удаляются; `FriendshipOfferPersistenceMapper` будет маппить сущность в модели порта.
- `PostgresErrors` в модуле friendships остаётся — им пользуется `FriendshipsPersistenceAdapter` (JDBC).
- В уроке тест вставки сравнивает сущности через `usingRecursiveComparison()` — так и сделаем.
