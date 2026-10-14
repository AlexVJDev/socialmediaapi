package ru.job4j.socialmediaapi.adapter.out.persistence.jdbc;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.job4j.socialmediaapi.adapter.out.persistence.entity.FriendshipsEntity;
import ru.job4j.socialmediaapi.adapter.out.persistence.mapper.FriendshipPersistenceMapper;
import ru.job4j.socialmediaapi.application.port.out.FriendshipsRepository;
import ru.job4j.socialmediaapi.domain.friendship.exception.FriendshipAlreadyExistsException;

import java.sql.Timestamp;
import java.util.UUID;

@Repository
public class FriendshipsRepositoryJdbc implements FriendshipsRepository {

    /** Inserts a friendship and returns the stored row. */
    private static final String CREATE = """
            INSERT INTO friendships (
                id, first_user_id, second_user_id,
                friendship_offer_id, status, created_at
            )
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id, first_user_id, second_user_id,
                      friendship_offer_id, status, created_at
            """;

    /** Reads a table row into an entity. */
    private static final RowMapper<FriendshipsEntity> ENTITY_MAPPER =
            (resultSet, rowNum) -> new FriendshipsEntity(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getObject("first_user_id", UUID.class),
                    resultSet.getObject("second_user_id", UUID.class),
                    resultSet.getObject("friendship_offer_id", UUID.class),
                    resultSet.getString("status"),
                    resultSet.getTimestamp("created_at").toInstant()
            );

    /** Executes the native SQL queries. */
    private final JdbcTemplate jdbcTemplate;

    /** Maps port requests/responses to entities and back. */
    private final FriendshipPersistenceMapper mapper;

    public FriendshipsRepositoryJdbc(
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
                    entity.friendshipOfferId(),
                    entity.status(),
                    Timestamp.from(entity.createdAt())
            );
            return mapper.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            if ("uq_friendships_users".equals(PostgresErrors.constraint(e))) {
                throw new FriendshipAlreadyExistsException(request.firstUserId(), request.secondUserId(), e);
            }
            throw e;
        }
    }
}
