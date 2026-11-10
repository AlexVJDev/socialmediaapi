package ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.persistence.row.SubscriptionRow;
import ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.persistence.mapper.SubscriptionPersistenceMapper;
import ru.job4j.socialmediaapi.subscriptions.internal.application.port.SubscriptionRepository;
import ru.job4j.socialmediaapi.subscriptions.internal.application.port.model.CreateSubscriptionRequest;
import ru.job4j.socialmediaapi.subscriptions.internal.application.port.model.SubscriptionResponse;
import ru.job4j.socialmediaapi.subscriptions.internal.application.exception.SubscriptionAlreadyExistsException;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC implementation of the {@link SubscriptionRepository} port: native SQL over the
 * {@code subscriptions} table.
 */
@Repository
@RequiredArgsConstructor
public class SubscriptionPersistenceAdapter implements SubscriptionRepository {

    /** Inserts a subscription and returns the stored row. */
    private static final String CREATE = """
            INSERT INTO subscriptions (
                id, follower_id, followed_id, created_at
            )
            VALUES (?, ?, ?, ?)
            RETURNING id, follower_id, followed_id, created_at
            """;

    /** Inserts a subscription unless it already exists; returns nothing on conflict. */
    private static final String CREATE_IF_ABSENT = """
            INSERT INTO subscriptions (
                id, follower_id, followed_id, created_at
            )
            VALUES (?, ?, ?, ?)
            ON CONFLICT (follower_id, followed_id) DO NOTHING
            RETURNING id, follower_id, followed_id, created_at
            """;

    /** Checks whether the follower is already subscribed to the followed user. */
    private static final String EXISTS = """
            SELECT EXISTS (
                SELECT 1
                FROM subscriptions
                WHERE follower_id = ? AND followed_id = ?
            )
            """;

    /** Removes the subscription in one direction. */
    private static final String DELETE = """
            DELETE FROM subscriptions
            WHERE follower_id = ? AND followed_id = ?
            """;

    /** Subscriptions of the follower, newest first (index {@code idx_subscriptions_follower}). */
    private static final String FIND_BY_FOLLOWER = """
            SELECT id, follower_id, followed_id, created_at
            FROM subscriptions
            WHERE follower_id = ?
            ORDER BY created_at DESC, id DESC
            """;

    /** Followers of the user, newest first (index {@code idx_subscriptions_followed}). */
    private static final String FIND_BY_FOLLOWED = """
            SELECT id, follower_id, followed_id, created_at
            FROM subscriptions
            WHERE followed_id = ?
            ORDER BY created_at DESC, id DESC
            """;

    /** Reads a result set row into {@link SubscriptionRow}. */
    private static final RowMapper<SubscriptionRow> ROW_MAPPER =
            (resultSet, rowNum) -> new SubscriptionRow(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getObject("follower_id", UUID.class),
                    resultSet.getObject("followed_id", UUID.class),
                    resultSet.getTimestamp("created_at").toInstant()
            );

    /** Executes the native SQL queries. */
    private final JdbcTemplate jdbcTemplate;

    /** Maps port requests/responses to rows and back. */
    private final SubscriptionPersistenceMapper mapper;

    @Override
    public SubscriptionResponse createSubscription(
            CreateSubscriptionRequest request
    ) {
        SubscriptionRow row = mapper.toRow(request);
        try {
            SubscriptionRow saved = jdbcTemplate.queryForObject(
                    CREATE,
                    ROW_MAPPER,
                    row.id(),
                    row.followerId(),
                    row.followedId(),
                    Timestamp.from(row.createdAt())
            );
            return mapper.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            if ("uq_subscriptions_users".equals(PostgresErrors.constraint(e))) {
                throw new SubscriptionAlreadyExistsException(request.followerId(), request.followedId(), e);
            }
            throw e;
        }
    }

    @Override
    public Optional<SubscriptionResponse> createSubscriptionIfAbsent(
            CreateSubscriptionRequest request
    ) {
        SubscriptionRow row = mapper.toRow(request);
        return jdbcTemplate.query(
                        CREATE_IF_ABSENT,
                        ROW_MAPPER,
                        row.id(),
                        row.followerId(),
                        row.followedId(),
                        Timestamp.from(row.createdAt())
                )
                .stream()
                .findFirst()
                .map(mapper::toResponse);
    }

    @Override
    public boolean existsSubscription(UUID followerId, UUID followedId) {
        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject(EXISTS, Boolean.class, followerId, followedId)
        );
    }

    @Override
    public boolean deleteSubscription(UUID followerId, UUID followedId) {
        return jdbcTemplate.update(DELETE, followerId, followedId) == 1;
    }

    @Override
    public List<SubscriptionResponse> findByFollower(UUID followerId) {
        return mapper.toResponses(jdbcTemplate.query(FIND_BY_FOLLOWER, ROW_MAPPER, followerId));
    }

    @Override
    public List<SubscriptionResponse> findByFollowed(UUID followedId) {
        return mapper.toResponses(jdbcTemplate.query(FIND_BY_FOLLOWED, ROW_MAPPER, followedId));
    }
}
