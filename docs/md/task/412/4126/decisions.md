# 4126 — журнал решений (Хранение и чтение пользовательской ленты)

Общие решения — `../4121/decisions.md`; JPA — `../4122/`, `../4124/`, `../4125/decisions.md`; тесты — `../4123/decisions.md`.

| ID | Решение (1–2 строки) | Источник | Статус |
|---|---|---|---|
| Р6-1 | Лента на JPA: `FeedJpaRepository extends Repository<PostEntity, UUID>` (`posts/…/persistence/jpa/`), нативный запрос → `PostEntity`; `PostRow`, `FeedPersistenceMapper` и его тест удаляем; маппинг — `PostPersistenceMapper.toFeedResponse/toCursor`. | fix.md в.1, 24.11.2026 | действует |
| Р6-2 | Запрос ленты — наш: курсор `(created_at, id)` + `CROSS JOIN LATERAL` (тот же SQL), не `LIMIT/OFFSET` урока; API ленты не меняется. | fix.md в.2, 24.11.2026 | действует |
| Р6-3 | Новой миграции индекса нет: частичный `idx_posts_published_author_created` (008) покрывает `(author_id, created_at DESC, id DESC)`. | fix.md в.3, 24.11.2026 | действует |
| Р6-4 | В тесте ленты подписка создаётся через `SubscriptionJpaRepository` (обращение к internal модуля subscriptions — только в тесте). | fix.md в.4, 24.11.2026 | действует |
| Р6-5 | Сценарий теста: подписка; у автора два PUBLISHED с одинаковым `created_at` и один удалённый (`deletePublished`); у другого — PUBLISHED. Лента — только два поста автора, порядок `id DESC`. | fix.md в.5, 24.11.2026 | действует |
| Р6-6 | `ApplicationModulesTest`: правило про `*Row` заменить правилом «`*Entity` и `*JpaRepository` лежат в `infrastructure.persistence.jpa`». | fix.md в.6, 24.11.2026 | действует |
| Р6-7 | Тесты (пишет Claude, запускает пользователь): новый `FeedJpaRepositoryTest`; `FeedPersistenceMapperTest` удаляем; `FeedPersistenceAdapterTest` не меняем. | fix.md в.7, 24.11.2026 | действует |
