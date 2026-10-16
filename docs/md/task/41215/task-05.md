# Задание 05. Интеграционный тест `PostRepositoryJdbcTest`

**Пункт задания:** 7 (первая часть; в задании — `JdbcPostRepositoryTest`).
**Зависит от:** задания 04.

## Цель

Проверить `PostRepositoryJdbc` на настоящем PostgreSQL: SQL, миграции `007`/`008`, ограничения таблицы и правило «менять может только автор».

## Контекст

- Образец — `SubscriptionRepositoryJdbcTest`: `@SpringBootTest` + `@Import(TestcontainersConfiguration.class)`, порт `@Autowired`, `JdbcTemplate` — для подготовки и проверки данных напрямую. Docker должен быть запущен; H2 не используем.
- Контейнер общий для всех тестов: в каждом тесте — новые `UUID.randomUUID()`, чтобы тесты не зависели друг от друга.
- Время — фиксированные `Instant` (`NOW`, `NOW.plusSeconds(...)`), не `Instant.now()`.

## Сценарии

Создание и чтение:
- `createPost` возвращает сохранённые данные (все поля, статус `PUBLISHED`);
- `findPublishedById` после создания возвращает ту же публикацию;
- `findPublishedById` несуществующего id → `Optional.empty()`;
- `findPublishedById` удалённой публикации → `Optional.empty()`.

Изменение:
- автор меняет `title` / `content` → новые значения и `updated_at`, `created_at` и `author_id` не изменились (проверить и ответ, и строку в БД);
- другой пользователь (`actorId` ≠ автор) → `Optional.empty()`, строка в БД **не изменилась**;
- изменение удалённой публикации → `Optional.empty()`;
- несуществующий id → `Optional.empty()`.

Логическое удаление:
- автор удаляет → `true`, в БД `status = 'DELETED'`, строка осталась, `updated_at` обновлён;
- другой пользователь → `false`, статус остался `PUBLISHED`;
- повторное удаление → `false`, данные не меняются.

Ограничения таблицы (через `jdbcTemplate.update` напрямую):
- статус вне `PUBLISHED|DELETED` → `DataIntegrityViolationException` (`chk_posts_status`);
- `title = NULL` → `DataIntegrityViolationException`.

## Критерии приёмки

- `./mvnw test -Dtest=PostRepositoryJdbcTest` зелёный.
- Каждый отказ (чужой автор, удалённая запись) проверяет ещё и то, что данные в БД не изменились.
