package ru.job4j.socialmediaapi.adapter.out.persistence.jdbc;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.job4j.socialmediaapi.adapter.out.persistence.entity.SubscriptionEntity;
import ru.job4j.socialmediaapi.adapter.out.persistence.mapper.SubscriptionPersistenceMapper;
import ru.job4j.socialmediaapi.application.port.out.SubscriptionRepository;
import ru.job4j.socialmediaapi.domain.subscription.exception.SubscriptionAlreadyExistsException;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SubscriptionRepositoryJdbc implements SubscriptionRepository {

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

    /** Reads a table row into an entity. */
    private static final RowMapper<SubscriptionEntity> ENTITY_MAPPER =
            (resultSet, rowNum) -> new SubscriptionEntity(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getObject("follower_id", UUID.class),
                    resultSet.getObject("followed_id", UUID.class),
                    resultSet.getTimestamp("created_at").toInstant()
            );

    /** Executes the native SQL queries. */
    private final JdbcTemplate jdbcTemplate;

    /** Maps port requests/responses to entities and back. */
    private final SubscriptionPersistenceMapper mapper;

    public SubscriptionRepositoryJdbc(
            JdbcTemplate jdbcTemplate,
            SubscriptionPersistenceMapper mapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    @Override
    public CreateSubscriptionResponse createSubscription(
            CreateSubscriptionRequest request
    ) {
        var entity = mapper.toEntity(request);
        try {
            var saved = jdbcTemplate.queryForObject(
                    CREATE,
                    ENTITY_MAPPER,
                    entity.id(),
                    entity.followerId(),
                    entity.followedId(),
                    Timestamp.from(entity.createdAt())
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
    public Optional<CreateSubscriptionResponse> createSubscriptionIfAbsent(
            CreateSubscriptionRequest request
    ) {
        var entity = mapper.toEntity(request);
        return jdbcTemplate.query(
                        CREATE_IF_ABSENT,
                        ENTITY_MAPPER,
                        entity.id(),
                        entity.followerId(),
                        entity.followedId(),
                        Timestamp.from(entity.createdAt())
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
}
