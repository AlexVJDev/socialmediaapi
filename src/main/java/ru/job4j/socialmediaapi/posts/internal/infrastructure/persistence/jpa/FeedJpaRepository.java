package ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Лента читателя: опубликованные посты авторов, на которых он подписан, сначала новые
 * ({@code created_at DESC, id DESC}). Отдельной таблицы ленты нет — она строится при чтении
 * из {@code subscriptions} и {@code posts}.
 *
 * <p>Один нативный запрос: для каждой подписки читателя {@code CROSS JOIN LATERAL} берёт не больше
 * {@code limit} новейших опубликованных постов автора из частичного индекса
 * {@code idx_posts_published_author_created} (только индекс, без чтения таблицы), из них остаются
 * {@code limit} новейших, и только для этих строк колонки поста читаются по первичному ключу.
 * Новейшие посты ленты всегда среди новейших постов своих авторов, поэтому ограничение на автора
 * ничего не теряет, а стоимость — около «подписки × limit» записей индекса, независимо от числа постов
 * и глубины страницы. Страницы — по курсору {@code (created_at, id)} последнего показанного поста.
 * {@code p.status = 'PUBLISHED'} — литерал: по нему планировщик выбирает частичный индекс.
 * См. {@code docs/process/Feed.md}.
 */
@Transactional(readOnly = true)
public interface FeedJpaRepository extends Repository<PostEntity, UUID> {

    /**
     * Первая страница ленты.
     *
     * @param followerId читатель
     * @param limit      число читаемых строк: и на автора, и на страницу
     * @return посты ленты, сначала новые
     */
    @Query(value = """
            WITH page AS (
                SELECT  lp.id
                    FROM subscriptions s
                        CROSS JOIN LATERAL (
                            SELECT  p.id,
                                    p.created_at
                                FROM posts p
                                WHERE p.author_id = s.followed_id
                                    AND p.status = 'PUBLISHED'
                                ORDER BY p.created_at DESC, p.id DESC
                                LIMIT :limit
                        ) lp
                    WHERE s.follower_id = :followerId
                    ORDER BY lp.created_at DESC, lp.id DESC
                    LIMIT :limit
            )
            SELECT  p.id,
                    p.author_id,
                    p.title,
                    p.content,
                    p.status,
                    p.created_at,
                    p.updated_at
                FROM page
                    JOIN posts p ON p.id = page.id
                ORDER BY p.created_at DESC, p.id DESC
            """, nativeQuery = true)
    List<PostEntity> findFeedFirstPage(
            @Param("followerId") UUID followerId,
            @Param("limit") int limit
    );

    /**
     * Страница ленты строго после курсора, в том же порядке; сравнение строк становится условием индекса.
     *
     * @param followerId     читатель
     * @param afterCreatedAt {@code created_at} последнего показанного поста
     * @param afterId        {@code id} последнего показанного поста
     * @param limit          число читаемых строк: и на автора, и на страницу
     * @return посты ленты после курсора, сначала новые
     */
    @Query(value = """
            WITH page AS (
                SELECT  lp.id
                    FROM subscriptions s
                        CROSS JOIN LATERAL (
                            SELECT  p.id,
                                    p.created_at
                                FROM posts p
                                WHERE p.author_id = s.followed_id
                                    AND p.status = 'PUBLISHED'
                                    AND (p.created_at, p.id) < (:afterCreatedAt, :afterId)
                                ORDER BY p.created_at DESC, p.id DESC
                                LIMIT :limit
                        ) lp
                    WHERE s.follower_id = :followerId
                    ORDER BY lp.created_at DESC, lp.id DESC
                    LIMIT :limit
            )
            SELECT  p.id,
                    p.author_id,
                    p.title,
                    p.content,
                    p.status,
                    p.created_at,
                    p.updated_at
                FROM page
                    JOIN posts p ON p.id = page.id
                ORDER BY p.created_at DESC, p.id DESC
            """, nativeQuery = true)
    List<PostEntity> findFeedPageAfter(
            @Param("followerId") UUID followerId,
            @Param("afterCreatedAt") Instant afterCreatedAt,
            @Param("afterId") UUID afterId,
            @Param("limit") int limit
    );
}
