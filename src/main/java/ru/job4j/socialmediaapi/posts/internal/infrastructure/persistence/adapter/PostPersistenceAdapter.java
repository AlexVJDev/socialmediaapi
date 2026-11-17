package ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.mapper.FeedPersistenceMapper;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.mapper.PostPersistenceMapper;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.row.PostRow;
import ru.job4j.socialmediaapi.posts.internal.application.port.PostRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.FeedRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.CreatePostRequest;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedCursor;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedPageResponse;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.UpdatePostRequest;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.DeletePostRequest;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.PostResponse;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Посты автора читаются страницами по курсору {@code (created_at, id)} из частичного индекса
 * {@code idx_posts_published_author_created}; лишняя {@code limit + 1}-я строка только сообщает,
 * что есть следующая страница (как в {@link FeedPersistenceAdapter}).
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

    /**
     * Первая страница опубликованных постов автора. Параметры: автор, лимит.
     * {@code status = 'PUBLISHED'} — литерал, чтобы планировщик выбрал частичный индекс.
     */
    private static final String FIND_BY_AUTHOR_FIRST_PAGE = """
            SELECT id, author_id, title, content,
                   status, created_at, updated_at
            FROM posts
            WHERE author_id = ? AND status = 'PUBLISHED'
            ORDER BY created_at DESC, id DESC
            LIMIT ?
            """;

    /** Страница опубликованных постов автора после курсора. Параметры: автор, {@code created_at}, {@code id}, лимит. */
    private static final String FIND_BY_AUTHOR_PAGE_AFTER = """
            SELECT id, author_id, title, content,
                   status, created_at, updated_at
            FROM posts
            WHERE author_id = ? AND status = 'PUBLISHED'
              AND (created_at, id) < (?, ?)
            ORDER BY created_at DESC, id DESC
            LIMIT ?
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

    /** Строки страницы → посты и курсор порта (те же модели, что у ленты). */
    private final FeedPersistenceMapper pageMapper;

    @Override
    public PostResponse createPost(CreatePostRequest request) {
        PostRow row = mapper.toRow(request);
        PostRow saved = jdbcTemplate.queryForObject(
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

    @Override
    public FeedPageResponse findPublishedByAuthor(UUID authorId, int limit) {
        checkLimit(limit);
        return findPage(limit, FIND_BY_AUTHOR_FIRST_PAGE, authorId, limit + 1);
    }

    @Override
    public FeedPageResponse findPublishedByAuthorAfter(UUID authorId, FeedCursor after, int limit) {
        Objects.requireNonNull(after, "after");
        checkLimit(limit);
        return findPage(limit, FIND_BY_AUTHOR_PAGE_AFTER,
                authorId, Timestamp.from(after.createdAt()), after.id(), limit + 1);
    }

    /**
     * Читает до {@code limit + 1} строк: лишняя строка не возвращается,
     * она только означает, что есть следующая страница.
     *
     * @param limit максимальное число постов на странице
     * @param sql   запрос страницы
     * @param args  параметры запроса
     * @return страница с курсором следующей страницы, если она есть
     */
    private FeedPageResponse findPage(int limit, String sql, Object... args) {
        List<PostRow> rows = jdbcTemplate.query(sql, ROW_MAPPER, args);
        boolean hasNext = rows.size() > limit;
        List<PostRow> pageRows = hasNext ? rows.subList(0, limit) : rows;
        Optional<FeedCursor> next = hasNext
                ? Optional.of(pageMapper.toCursor(pageRows.getLast()))
                : Optional.empty();
        return new FeedPageResponse(
                pageRows.stream().map(pageMapper::toResponse).toList(),
                next
        );
    }

    /**
     * Отклоняет размер страницы вне допустимого до запроса.
     *
     * @param limit запрошенный размер страницы
     * @throws IllegalArgumentException {@code limit} меньше 1 или больше {@link FeedRepository#MAX_LIMIT}
     */
    private static void checkLimit(int limit) {
        if (limit < 1 || limit > FeedRepository.MAX_LIMIT) {
            throw new IllegalArgumentException(
                    "Page limit must be from 1 to %d, got %d".formatted(FeedRepository.MAX_LIMIT, limit));
        }
    }
}
