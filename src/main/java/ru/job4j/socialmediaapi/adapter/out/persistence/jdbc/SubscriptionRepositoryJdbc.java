package ru.job4j.socialmediaapi.adapter.out.persistence.jdbc;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.job4j.socialmediaapi.adapter.out.persistence.entity.SubscriptionEntity;
import ru.job4j.socialmediaapi.adapter.out.persistence.mapper.SubscriptionPersistenceMapper;
import ru.job4j.socialmediaapi.application.port.out.SubscriptionRepository;
import ru.job4j.socialmediaapi.domain.subscription.exception.SubscriptionAlreadyExistsException;
import ru.job4j.socialmediaapi.domain.subscription.exception.SubscriptionToSelfException;
import ru.job4j.socialmediaapi.domain.subscription.exception.SubscriptionUserRequiredException;

import java.sql.Timestamp;
import java.util.UUID;

@Repository
public class SubscriptionRepositoryJdbc implements SubscriptionRepository {

    private static final String CREATE = """
            INSERT INTO subscriptions (
                id, follower_id, followed_id, created_at
            )
            VALUES (?, ?, ?, ?)
            RETURNING id, follower_id, followed_id, created_at
            """;

    private static final String EXISTS = """
            SELECT EXISTS (
                SELECT 1
                FROM subscriptions
                WHERE follower_id = ? AND followed_id = ?
            )
            """;

    private static final RowMapper<SubscriptionEntity> ENTITY_MAPPER =
            (resultSet, rowNum) -> new SubscriptionEntity(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getObject("follower_id", UUID.class),
                    resultSet.getObject("followed_id", UUID.class),
                    resultSet.getTimestamp("created_at").toInstant()
            );

    private final JdbcTemplate jdbcTemplate;

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
            throw translate(request, e);
        }
    }

    @Override
    public boolean existsSubscription(UUID followerId, UUID followedId) {
        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject(EXISTS, Boolean.class, followerId, followedId)
        );
    }

    private static RuntimeException translate(
            CreateSubscriptionRequest request,
            DataIntegrityViolationException exception
    ) {
        var notNullColumn = PostgresErrors.notNullColumn(exception);
        if (notNullColumn.isPresent()) {
            return new SubscriptionUserRequiredException(notNullColumn.get(), exception);
        }
        return switch (PostgresErrors.constraint(exception)) {
            case "uq_subscriptions_users" -> new SubscriptionAlreadyExistsException(
                    request.followerId(), request.followedId(), exception
            );
            case "chk_subscriptions_different_users" -> new SubscriptionToSelfException(
                    request.followerId(), exception
            );
            default -> exception;
        };
    }
}
