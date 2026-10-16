package ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.mapper.FeedPersistenceMapper;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.row.PostRow;
import ru.job4j.socialmediaapi.posts.internal.application.port.FeedRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedPostResponse;

import java.util.List;
import java.util.UUID;

/**
 * The feed is one query over {@code subscriptions} and {@code posts}. {@code id} is the second
 * sort key: posts may share {@code created_at}, and without it their order, and so the pages,
 * would not be stable. Pages use {@code LIMIT}/{@code OFFSET}; its limits are described
 * in {@code docs/process/Feed.md}.
 */
@Repository
@RequiredArgsConstructor
public class FeedPersistenceAdapter implements FeedRepository {

    /** Selects a page of published posts of the authors the follower is subscribed to. */
    private static final String FIND_FEED = """
            SELECT p.id, p.author_id, p.title, p.content,
                   p.status, p.created_at, p.updated_at
            FROM subscriptions s
            JOIN posts p ON p.author_id = s.followed_id
            WHERE s.follower_id = ?
              AND p.status = 'PUBLISHED'
            ORDER BY p.created_at DESC, p.id DESC
            LIMIT ? OFFSET ?
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

    /** Maps rows to port responses. */
    private final FeedPersistenceMapper mapper;

    @Override
    public List<FeedPostResponse> findFeed(UUID followerId, int limit, int offset) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Feed limit must be positive, got %d".formatted(limit));
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Feed offset must not be negative, got %d".formatted(offset));
        }
        return jdbcTemplate.query(FIND_FEED, ROW_MAPPER, followerId, limit, offset)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}
