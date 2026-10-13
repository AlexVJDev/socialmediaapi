package ru.job4j.socialmediaapi.adapter.out.persistence.jdbc;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.job4j.socialmediaapi.adapter.out.persistence.entity.OfferFriendshipEntity;
import ru.job4j.socialmediaapi.adapter.out.persistence.mapper.OfferFriendshipPersistenceMapper;
import ru.job4j.socialmediaapi.application.port.out.OfferFriendshipRepository;
import ru.job4j.socialmediaapi.domain.offerfriendship.exception.OfferFriendshipToSelfException;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Repository
public class FriendshipOfferRepositoryJdbc implements OfferFriendshipRepository {

    /** Inserts an offer unless one already exists for the pair; returns nothing on conflict. */
    private static final String CREATE_IF_ABSENT = """
            INSERT INTO offer_friendships (
                id, from_user_id, to_user_id,
                status, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (from_user_id, to_user_id) DO NOTHING
            RETURNING id, from_user_id, to_user_id,
                      status, created_at, updated_at
            """;

    /** Selects the offer for the pair and locks it until the end of the transaction. */
    private static final String FIND_BY_USERS = """
            SELECT id, from_user_id, to_user_id,
                   status, created_at, updated_at
            FROM offer_friendships
            WHERE from_user_id = ? AND to_user_id = ?
            FOR UPDATE
            """;

    /** Reads a table row into an entity. */
    private static final RowMapper<OfferFriendshipEntity> ENTITY_MAPPER =
            (resultSet, rowNum) -> new OfferFriendshipEntity(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getObject("from_user_id", UUID.class),
                    resultSet.getObject("to_user_id", UUID.class),
                    resultSet.getString("status"),
                    resultSet.getTimestamp("created_at").toInstant(),
                    resultSet.getTimestamp("updated_at").toInstant()
            );

    /** Executes the native SQL queries. */
    private final JdbcTemplate jdbcTemplate;

    /** Maps port requests/responses to entities and back. */
    private final OfferFriendshipPersistenceMapper mapper;

    public FriendshipOfferRepositoryJdbc(
            JdbcTemplate jdbcTemplate,
            OfferFriendshipPersistenceMapper mapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    @Override
    public Optional<CreateOfferFriendshipResponse> createOfferFriendshipIfAbsent(
            CreateOfferFriendshipRequest request
    ) {
        var entity = mapper.toEntity(request);
        try {
            return jdbcTemplate.query(
                            CREATE_IF_ABSENT,
                            ENTITY_MAPPER,
                            entity.id(),
                            entity.fromUserId(),
                            entity.toUserId(),
                            entity.status(),
                            Timestamp.from(entity.createdAt()),
                            Timestamp.from(entity.updatedAt())
                    )
                    .stream()
                    .findFirst()
                    .map(mapper::toResponse);
        } catch (DataIntegrityViolationException e) {
            throw translate(request, e);
        }
    }

    @Override
    public Optional<FindOfferFriendshipResponse> findByUsers(UUID fromUserId, UUID toUserId) {
        return jdbcTemplate.query(FIND_BY_USERS, ENTITY_MAPPER, fromUserId, toUserId)
                .stream()
                .findFirst()
                .map(mapper::toFindResponse);
    }

    private static RuntimeException translate(
            CreateOfferFriendshipRequest request,
            DataIntegrityViolationException exception
    ) {
        return "chk_offer_friendships_different_users".equals(PostgresErrors.constraint(exception))
                ? new OfferFriendshipToSelfException(request.fromUserId(), exception)
                : exception;
    }
}
