# 4125 — заметки анализа

Урок: миграция `posts` (CHECK статуса, rollback), `PostEntity`, `PostRepository extends JpaRepository`,
создание через `save`, чтение `findByIdAndStatus(id, PUBLISHED)`, логическое удаление — статус `DELETED`, строка остаётся.
Тест: save → найти → сохранить с DELETED → не находится.

## Порт PostRepository → JPA
| Метод | Сейчас (JDBC) | JPA |
|---|---|---|
| `createPost` | `INSERT … RETURNING` | `save(PostEntity)`; `Persistable.isNew()` = true → `persist` без `SELECT` |
| `findPublishedById` | `WHERE id = ? AND status = 'PUBLISHED'` | `findByIdAndStatus(id, PUBLISHED)` |
| `updatePost` | `UPDATE … WHERE id, author_id, PUBLISHED RETURNING` | `@Modifying` JPQL → 1 ⇒ `findById` |
| `deletePost` | `UPDATE status='DELETED' … WHERE id, author_id, PUBLISHED` | `@Modifying` JPQL → `== 1` |
| `findPublishedByAuthor` / `…After` | native, курсор, литерал `'PUBLISHED'` | те же native-запросы, результат — `PostEntity` |

## Persistable
`SimpleJpaRepository.save` вызывает `persist`, если `isNew()`, иначе `merge` (с `SELECT`). Без `@Version` и при
заданном id сущность считается не новой. `PostEntity implements Persistable<UUID>`: поле `@Transient boolean isNew`
= true у созданной в коде сущности; после `@PostPersist` / `@PostLoad` — false.

## Лента (4126)
`FeedPersistenceAdapter` (JDBC, `CROSS JOIN LATERAL`) и его `PostRow`/`FeedPersistenceMapper` остаются до 4126.
