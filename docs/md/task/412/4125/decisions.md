# 4125 — журнал решений (Публикации и пользовательская лента)

Общие решения — `../4121/decisions.md`; JPA — `../4122/decisions.md`, `../4124/decisions.md`; тесты — `../4123/decisions.md`.

| ID | Решение (1–2 строки) | Источник | Статус |
|---|---|---|---|
| Р5-1 | На JPA переводим `PostPersistenceAdapter` (таблица `posts`); лента (`FeedPersistenceAdapter`), `PostRow`, `FeedPersistenceMapper` — до 4126. | fix.md в.1, 24.11.2026 | действует |
| Р5-2 | `PostEntity` + `PostJpaRepository` в `posts/…/persistence/jpa/`; `Repository<PostEntity, UUID>`; статус — доменный `PostStatus`; транзакции как Р2-11. | fix.md в.2, 24.11.2026 | действует |
| Р5-3 | Создание — `save`; `PostEntity implements Persistable<UUID>` (новая сущность → `persist`, без `SELECT`). | fix.md в.3, 24.11.2026 | действует |
| Р5-4 | Чтение — `findByIdAndStatus(id, PostStatus.PUBLISHED)`. | fix.md в.4, 24.11.2026 | действует |
| Р5-5 | Изменение и логическое удаление — атомарный `@Modifying` JPQL `UPDATE … WHERE id, authorId, status = PUBLISHED`; у сущности нет сеттеров; в тесте «save с DELETED» заменён логическим удалением методом репозитория. | fix.md в.5, 24.11.2026 | действует |
| Р5-6 | Страницы постов автора — нативный SQL с курсором и литералом `'PUBLISHED'` (частичный индекс 008). | fix.md в.6, 24.11.2026 | действует |
| Р5-7 | `PostPersistenceMapper`: `PostEntity` → `PostResponse`, `FeedPostResponse`, `FeedCursor`; `toRow` убрать; `FeedPersistenceMapper` не трогаем. | fix.md в.7, 24.11.2026 | действует |
| Р5-8 | Тесты (пишет Claude, запускает пользователь): `PostJpaRepositoryTest` (сценарий урока); `PostPersistenceMapperTest` — под сущность; `PostPersistenceAdapterTest` не меняем. | fix.md в.8, 24.11.2026 | действует |
