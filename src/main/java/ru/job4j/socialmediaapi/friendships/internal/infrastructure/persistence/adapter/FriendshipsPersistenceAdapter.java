package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.row.FriendRow;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.row.FriendshipsRow;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.mapper.FriendshipPersistenceMapper;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipsRepository;
import ru.job4j.socialmediaapi.friendships.internal.application.exception.FriendshipAlreadyExistsException;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FindFriendResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendCursor;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendPageResponse;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC-реализация порта {@link FriendshipsRepository}: нативный SQL по таблице
 * {@code friendships}.
 */
@Repository
@RequiredArgsConstructor
public class FriendshipsPersistenceAdapter implements FriendshipsRepository {

    /** Вставляет дружбу и возвращает сохранённую строку. */
    private static final String CREATE = """
            INSERT INTO friendships (
                id, first_user_id, second_user_id,
                friendship_offer_id, status, created_at
            )
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id, first_user_id, second_user_id,
                      friendship_offer_id, status, created_at
            """;

    /**
     * Первая страница активных друзей пользователя. Пользователь может быть любой стороной дружбы, поэтому
     * страница — слияние двух веток, каждая читается по своему индексу ({@code idx_friendships_first_user_page},
     * {@code idx_friendships_second_user_page}) и обрезается до размера страницы до слияния.
     * Параметры: пользователь, число строк, пользователь, число строк, число строк.
     */
    private static final String FIND_FRIENDS_FIRST_PAGE = """
            (SELECT id, second_user_id AS friend_id, created_at
             FROM friendships
             WHERE first_user_id = ? AND status = 'ACTIVE'
             ORDER BY created_at DESC, id DESC
             LIMIT ?)
            UNION ALL
            (SELECT id, first_user_id AS friend_id, created_at
             FROM friendships
             WHERE second_user_id = ? AND status = 'ACTIVE'
             ORDER BY created_at DESC, id DESC
             LIMIT ?)
            ORDER BY created_at DESC, id DESC
            LIMIT ?
            """;

    /**
     * Страница активных друзей пользователя после курсора. Параметры: пользователь, {@code created_at} курсора,
     * {@code id} курсора, число строк — для каждой ветки, затем число строк общей страницы.
     */
    private static final String FIND_FRIENDS_PAGE_AFTER = """
            (SELECT id, second_user_id AS friend_id, created_at
             FROM friendships
             WHERE first_user_id = ? AND status = 'ACTIVE'
               AND (created_at, id) < (?, ?)
             ORDER BY created_at DESC, id DESC
             LIMIT ?)
            UNION ALL
            (SELECT id, first_user_id AS friend_id, created_at
             FROM friendships
             WHERE second_user_id = ? AND status = 'ACTIVE'
               AND (created_at, id) < (?, ?)
             ORDER BY created_at DESC, id DESC
             LIMIT ?)
            ORDER BY created_at DESC, id DESC
            LIMIT ?
            """;

    /** Читает строку страницы друзей в {@link FriendRow}. */
    private static final RowMapper<FriendRow> FRIEND_ROW_MAPPER =
            (resultSet, rowNum) -> new FriendRow(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getObject("friend_id", UUID.class),
                    resultSet.getTimestamp("created_at").toInstant()
            );

    /** Читает строку результата в {@link FriendshipsRow}. */
    private static final RowMapper<FriendshipsRow> ROW_MAPPER =
            (resultSet, rowNum) -> new FriendshipsRow(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getObject("first_user_id", UUID.class),
                    resultSet.getObject("second_user_id", UUID.class),
                    resultSet.getObject("friendship_offer_id", UUID.class),
                    resultSet.getString("status"),
                    resultSet.getTimestamp("created_at").toInstant()
            );

    /** Выполняет нативные SQL-запросы. */
    private final JdbcTemplate jdbcTemplate;

    /** Преобразует запросы/ответы порта в строки и обратно. */
    private final FriendshipPersistenceMapper mapper;

    @Override
    public CreateFriendshipResponse createFriendship(
            CreateFriendshipRequest request
    ) {
        FriendshipsRow row = mapper.toRow(request);
        try {
            FriendshipsRow saved = jdbcTemplate.queryForObject(
                    CREATE,
                    ROW_MAPPER,
                    row.id(),
                    row.firstUserId(),
                    row.secondUserId(),
                    row.friendshipOfferId(),
                    row.status(),
                    Timestamp.from(row.createdAt())
            );
            return mapper.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            if ("uq_friendships_users".equals(PostgresErrors.constraint(e))) {
                throw new FriendshipAlreadyExistsException(request.firstUserId(), request.secondUserId(), e);
            }
            throw e;
        }
    }

    @Override
    public FriendPageResponse findFriendsPage(UUID userId, int limit) {
        Objects.requireNonNull(userId, "userId");
        checkLimit(limit);
        int fetch = limit + 1;
        return findFriendsPage(limit, FIND_FRIENDS_FIRST_PAGE, userId, fetch, userId, fetch, fetch);
    }

    @Override
    public FriendPageResponse findFriendsPageAfter(UUID userId, FriendCursor after, int limit) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(after, "after");
        checkLimit(limit);
        int fetch = limit + 1;
        Timestamp createdAt = Timestamp.from(after.createdAt());
        return findFriendsPage(limit, FIND_FRIENDS_PAGE_AFTER,
                userId, createdAt, after.id(), fetch,
                userId, createdAt, after.id(), fetch,
                fetch);
    }

    /**
     * Читает до {@code limit + 1} строк: лишняя строка не возвращается,
     * она лишь означает, что есть следующая страница.
     *
     * @param limit максимальное число друзей на странице
     * @param sql   запрос страницы
     * @param args  параметры запроса
     * @return страница с курсором следующей, если она есть
     */
    private FriendPageResponse findFriendsPage(int limit, String sql, Object... args) {
        List<FriendRow> rows = jdbcTemplate.query(sql, FRIEND_ROW_MAPPER, args);
        boolean hasNext = rows.size() > limit;
        List<FriendRow> pageRows = hasNext ? rows.subList(0, limit) : rows;
        Optional<FriendCursor> next = hasNext
                ? Optional.of(new FriendCursor(pageRows.getLast().createdAt(), pageRows.getLast().id()))
                : Optional.empty();
        return new FriendPageResponse(
                pageRows.stream().map(row -> new FindFriendResponse(row.id(), row.friendId())).toList(),
                next
        );
    }

    /**
     * Отклоняет размер страницы, который хранилище не обслуживает, до запроса.
     *
     * @param limit запрошенный размер страницы
     * @throws IllegalArgumentException {@code limit} меньше 1 или больше {@link #MAX_LIMIT}
     */
    private static void checkLimit(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException(
                    "Page limit must be from 1 to %d, got %d".formatted(MAX_LIMIT, limit));
        }
    }
}
