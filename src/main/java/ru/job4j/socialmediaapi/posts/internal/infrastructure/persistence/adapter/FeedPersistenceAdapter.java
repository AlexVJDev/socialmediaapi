package ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.job4j.socialmediaapi.posts.internal.application.port.FeedRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedCursor;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedPageResponse;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.mapper.FeedPersistenceMapper;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.row.PostRow;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The feed is one query: for every subscription of the reader {@code CROSS JOIN LATERAL} takes
 * at most {@code limit + 1} newest published posts of the author from the partial index
 * {@code idx_posts_published_author_created} (index only, no table reads), then the newest
 * {@code limit + 1} of them are kept and only for these rows the post columns are read
 * by the primary key. The newest posts of the feed are always among the newest posts of
 * their authors, so the per-author limit loses nothing, and the cost is about
 * subscriptions × limit index entries, whatever the number of posts and the page depth.
 * Pages are read by the cursor {@code (created_at, id)} of the last shown post; the extra
 * {@code limit + 1}-th row only tells that there is a next page. See {@code docs/process/Feed.md}.
 */
@Repository
@RequiredArgsConstructor
public class FeedPersistenceAdapter implements FeedRepository {

    /**
     * First page of the feed. Parameters: per-author limit, follower, page limit.
     * {@code p.status = 'PUBLISHED'} must stay literal: it lets the planner use the partial index.
     */
    private static final String FIND_FIRST_PAGE = """
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
                                LIMIT ?
                        ) lp
                    WHERE s.follower_id = ?
                    ORDER BY lp.created_at DESC, lp.id DESC
                    LIMIT ?
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
            """;

    /**
     * Page of the feed after the cursor. Parameters: cursor {@code created_at}, cursor {@code id},
     * per-author limit, follower, page limit. The row comparison becomes an index condition.
     */
    private static final String FIND_PAGE_AFTER = """
            WITH page AS (
                SELECT  lp.id
                    FROM subscriptions s
                        CROSS JOIN LATERAL (
                            SELECT  p.id,
                                    p.created_at
                                FROM posts p
                                WHERE p.author_id = s.followed_id
                                    AND p.status = 'PUBLISHED'
                                    AND (p.created_at, p.id) < (?, ?)
                                ORDER BY p.created_at DESC, p.id DESC
                                LIMIT ?
                        ) lp
                    WHERE s.follower_id = ?
                    ORDER BY lp.created_at DESC, lp.id DESC
                    LIMIT ?
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
            """;

    /** Reads a result set row into {@link PostRow}. */
    private static final RowMapper<PostRow> ROW_MAPPER =
            (resultSet, rowNum) -> new PostRow(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getObject("author_id", UUID.class),
                    resultSet.getString("title"),
                    resultSet.getString("content"),
                    resultSet.getString("status"),
                    resultSet.getTimestamp("created_at").toInstant(),
                    resultSet.getTimestamp("updated_at").toInstant()
            );

    /** Executes the native SQL queries. */
    private final JdbcTemplate jdbcTemplate;

    /** Maps rows to port responses and cursors. */
    private final FeedPersistenceMapper mapper;

    @Override
    public FeedPageResponse findFeed(UUID followerId, int limit) {
        checkLimit(limit);
        var fetch = limit + 1;
        return findPage(limit, FIND_FIRST_PAGE, fetch, followerId, fetch);
    }

    @Override
    public FeedPageResponse findFeedAfter(UUID followerId, FeedCursor after, int limit) {
        Objects.requireNonNull(after, "after");
        checkLimit(limit);
        var fetch = limit + 1;
        return findPage(limit, FIND_PAGE_AFTER,
                Timestamp.from(after.createdAt()), after.id(), fetch, followerId, fetch);
    }

    /**
     * Reads up to {@code limit + 1} rows: the extra row is not returned,
     * it only means that there is a next page.
     *
     * @param limit maximum number of posts on the page
     * @param sql   query of the page
     * @param args  query parameters
     * @return the page with the cursor of the next page, if there is one
     */
    private FeedPageResponse findPage(int limit, String sql, Object... args) {
        var rows = jdbcTemplate.query(sql, ROW_MAPPER, args);
        var hasNext = rows.size() > limit;
        List<PostRow> pageRows = hasNext ? rows.subList(0, limit) : rows;
        var next = hasNext
                ? Optional.of(mapper.toCursor(pageRows.getLast()))
                : Optional.<FeedCursor>empty();
        return new FeedPageResponse(
                pageRows.stream().map(mapper::toResponse).toList(),
                next
        );
    }

    /**
     * Rejects a page size the feed does not serve, before the query.
     *
     * @param limit requested page size
     * @throws IllegalArgumentException {@code limit} is less than 1 or greater than {@link #MAX_LIMIT}
     */
    private static void checkLimit(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException(
                    "Feed limit must be from 1 to %d, got %d".formatted(MAX_LIMIT, limit));
        }
    }
}
