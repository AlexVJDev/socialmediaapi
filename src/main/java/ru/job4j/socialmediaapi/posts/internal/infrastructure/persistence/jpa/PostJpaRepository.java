package ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Запросы Spring Data JPA к таблице {@code posts} для операций с публикациями (лента — отдельно).
 *
 * <p>Наследует {@link Repository}, а не {@code JpaRepository}: доступны только объявленные методы,
 * запросов по всей таблице нет. Каждый запрос ограничен {@code id} или автором.
 *
 * <p>Транзакции — как у {@code SimpleJpaRepository}: чтения {@code readOnly}, запись — в транзакции записи.
 * Метод присоединяется к транзакции вызывающего сценария, а без неё открывает свою.
 */
@Transactional(readOnly = true)
public interface PostJpaRepository extends Repository<PostEntity, UUID> {

    /**
     * Сохраняет публикацию; новая ({@link PostEntity#isNew()}) вставляется одним {@code INSERT}.
     *
     * @param post публикация
     * @return сохранённая публикация
     */
    @Transactional
    PostEntity save(PostEntity post);

    /**
     * @param id id публикации
     * @return публикация в любом статусе; пусто, если её нет
     */
    Optional<PostEntity> findById(UUID id);

    /**
     * Обычное чтение публикации: передаётся {@link PostStatus#PUBLISHED}, удалённая не находится.
     *
     * @param id     id публикации
     * @param status статус
     * @return публикация в этом статусе; пусто, если её нет или статус другой
     */
    Optional<PostEntity> findByIdAndStatus(UUID id, PostStatus status);

    /**
     * Меняет заголовок и текст опубликованной публикации автора одним запросом.
     *
     * @param id        id публикации
     * @param authorId  пользователь, который меняет: должен быть автором
     * @param title     новый заголовок
     * @param content   новый текст
     * @param updatedAt время изменения
     * @return 1 — изменена; 0 — нет, автор другой или публикация удалена
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE PostEntity p
            SET p.title = :title, p.content = :content, p.updatedAt = :updatedAt
            WHERE p.id = :id AND p.authorId = :authorId
              AND p.status = ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus.PUBLISHED
            """)
    int updatePublished(
            @Param("id") UUID id,
            @Param("authorId") UUID authorId,
            @Param("title") String title,
            @Param("content") String content,
            @Param("updatedAt") Instant updatedAt
    );

    /**
     * Логически удаляет опубликованную публикацию автора: статус {@code DELETED}, строка остаётся.
     *
     * @param id        id публикации
     * @param authorId  пользователь, который удаляет: должен быть автором
     * @param deletedAt время удаления
     * @return 1 — удалена; 0 — нет, автор другой или уже удалена
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE PostEntity p
            SET p.status = ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus.DELETED,
                p.updatedAt = :deletedAt
            WHERE p.id = :id AND p.authorId = :authorId
              AND p.status = ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus.PUBLISHED
            """)
    int deletePublished(
            @Param("id") UUID id,
            @Param("authorId") UUID authorId,
            @Param("deletedAt") Instant deletedAt
    );

    /**
     * Первая страница опубликованных постов автора, сначала новые. {@code status = 'PUBLISHED'} — литерал,
     * чтобы планировщик выбрал частичный индекс {@code idx_posts_published_author_created}.
     *
     * @param authorId автор
     * @param limit    число читаемых строк
     * @return публикации автора
     */
    @Query(value = """
            SELECT id, author_id, title, content,
                   status, created_at, updated_at
            FROM posts
            WHERE author_id = :authorId AND status = 'PUBLISHED'
            ORDER BY created_at DESC, id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<PostEntity> findPublishedByAuthorFirstPage(
            @Param("authorId") UUID authorId,
            @Param("limit") int limit
    );

    /**
     * Страница опубликованных постов автора строго после курсора, в том же порядке.
     *
     * @param authorId       автор
     * @param afterCreatedAt {@code created_at} последней показанной публикации
     * @param afterId        {@code id} последней показанной публикации
     * @param limit          число читаемых строк
     * @return публикации автора после курсора
     */
    @Query(value = """
            SELECT id, author_id, title, content,
                   status, created_at, updated_at
            FROM posts
            WHERE author_id = :authorId AND status = 'PUBLISHED'
              AND (created_at, id) < (:afterCreatedAt, :afterId)
            ORDER BY created_at DESC, id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<PostEntity> findPublishedByAuthorPageAfter(
            @Param("authorId") UUID authorId,
            @Param("afterCreatedAt") Instant afterCreatedAt,
            @Param("afterId") UUID afterId,
            @Param("limit") int limit
    );
}
