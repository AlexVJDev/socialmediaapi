package ru.job4j.socialmediaapi.adapter.out.persistence.jdbc;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.job4j.socialmediaapi.adapter.out.persistence.entity.FriendshipEntity;
import ru.job4j.socialmediaapi.adapter.out.persistence.mapper.FriendshipPersistenceMapper;
import ru.job4j.socialmediaapi.application.port.out.FriendshipRepository;
import ru.job4j.socialmediaapi.domain.friendship.exception.FriendshipAlreadyExistsException;
import ru.job4j.socialmediaapi.domain.friendship.exception.FriendshipInvalidUserOrderException;
import ru.job4j.socialmediaapi.domain.offerfriendship.exception.FriendshipRequestNotFoundException;

import java.sql.Timestamp;
import java.util.UUID;

@Repository
public class FriendshipRepositoryJdbc implements FriendshipRepository {

    /** Inserts a friendship and returns the stored row. */
    private static final String CREATE = """
            INSERT INTO friendships (
                id, first_user_id, second_user_id,
                offer_friendship_id, status, created_at
            )
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id, first_user_id, second_user_id,
                      offer_friendship_id, status, created_at
            """;

    /** Reads a table row into an entity. */
    private static final RowMapper<FriendshipEntity> ENTITY_MAPPER =
            (resultSet, rowNum) -> new FriendshipEntity(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getObject("first_user_id", UUID.class),
                    resultSet.getObject("second_user_id", UUID.class),
                    resultSet.getObject("offer_friendship_id", UUID.class),
                    resultSet.getString("status"),
                    resultSet.getTimestamp("created_at").toInstant()
            );

    /** Executes the native SQL queries. */
    private final JdbcTemplate jdbcTemplate;

    /** Maps port requests/responses to entities and back. */
    private final FriendshipPersistenceMapper mapper;

    public FriendshipRepositoryJdbc(
            JdbcTemplate jdbcTemplate,
            FriendshipPersistenceMapper mapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    @Override
    public CreateFriendshipResponse createFriendship(
            CreateFriendshipRequest request
    ) {
        var entity = mapper.toEntity(request);
        try {
            var saved = jdbcTemplate.queryForObject(
                    CREATE,
                    ENTITY_MAPPER,
                    entity.id(),
                    entity.firstUserId(),
                    entity.secondUserId(),
                    entity.offerFriendshipId(),
                    entity.status(),
                    Timestamp.from(entity.createdAt())
            );
            return mapper.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw translate(request, e);
        }
    }

    private static RuntimeException translate(
            CreateFriendshipRequest request,
            DataIntegrityViolationException exception
    ) {
        return switch (PostgresErrors.constraint(exception)) {
            case "uq_friendships_users" -> new FriendshipAlreadyExistsException(
                    request.firstUserId(), request.secondUserId(), exception
            );
            case "chk_friendships_canonical_order" -> new FriendshipInvalidUserOrderException(
                    request.firstUserId(), request.secondUserId(), exception
            );
            case "fk_friendships_offer_friendship" -> new FriendshipRequestNotFoundException(
                    request.offerFriendshipId(), exception
            );
            default -> exception;
        };
    }
}
