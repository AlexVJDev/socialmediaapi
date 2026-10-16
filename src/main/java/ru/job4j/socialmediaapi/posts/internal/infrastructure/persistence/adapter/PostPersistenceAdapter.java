package ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.mapper.PostPersistenceMapper;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.row.PostRow;
import ru.job4j.socialmediaapi.posts.internal.application.port.PostRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.CreatePostRequest;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.UpdatePostRequest;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.DeletePostRequest;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.PostResponse;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

/**
 * Only the author changes or deletes a post: the author and the status are checked
 * in the {@code WHERE} of the same {@code UPDATE}, not by a separate read before it,
 * so the check and the change are atomic.
 */
@Repository
@RequiredArgsConstructor
public class PostPersistenceAdapter implements PostRepository {

    /** Inserts a post and returns the stored row. */
    private static final String CREATE = """
            INSERT INTO posts (
                id, author_id, title, content,
                status, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING id, author_id, title, content,
                      status, created_at, updated_at
            """;

    /** Selects a post unless it is deleted. */
    private static final String FIND_PUBLISHED_BY_ID = """
            SELECT id, author_id, title, content,
                   status, created_at, updated_at
            FROM posts
            WHERE id = ? AND status = 'PUBLISHED'
            """;

    /** Changes a published post of the author and returns the stored row. */
    private static final String UPDATE = """
            UPDATE posts
            SET title = ?, content = ?, updated_at = ?
            WHERE id = ? AND author_id = ? AND status = 'PUBLISHED'
            RETURNING id, author_id, title, content,
                      status, created_at, updated_at
            """;

    /** Marks a published post of the author as deleted; the row is kept. */
    private static final String DELETE = """
            UPDATE posts
            SET status = 'DELETED', updated_at = ?
            WHERE id = ? AND author_id = ? AND status = 'PUBLISHED'
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

    /** Maps port requests/responses to rows and back. */
    private final PostPersistenceMapper mapper;

    @Override
    public PostResponse createPost(CreatePostRequest request) {
        var row = mapper.toRow(request);
        var saved = jdbcTemplate.queryForObject(
                CREATE,
                ROW_MAPPER,
                row.id(),
                row.authorId(),
                row.title(),
                row.content(),
                row.status(),
                Timestamp.from(row.createdAt()),
                Timestamp.from(row.updatedAt())
        );
        return mapper.toResponse(saved);
    }

    @Override
    public Optional<PostResponse> findPublishedById(UUID postId) {
        return jdbcTemplate.query(FIND_PUBLISHED_BY_ID, ROW_MAPPER, postId)
                .stream()
                .findFirst()
                .map(mapper::toResponse);
    }

    @Override
    public Optional<PostResponse> updatePost(UpdatePostRequest request) {
        return jdbcTemplate.query(
                        UPDATE,
                        ROW_MAPPER,
                        request.title(),
                        request.content(),
                        Timestamp.from(request.updatedAt()),
                        request.postId(),
                        request.actorId()
                )
                .stream()
                .findFirst()
                .map(mapper::toResponse);
    }

    @Override
    public boolean deletePost(DeletePostRequest request) {
        return jdbcTemplate.update(
                DELETE,
                Timestamp.from(request.deletedAt()),
                request.postId(),
                request.actorId()
        ) == 1;
    }
}
