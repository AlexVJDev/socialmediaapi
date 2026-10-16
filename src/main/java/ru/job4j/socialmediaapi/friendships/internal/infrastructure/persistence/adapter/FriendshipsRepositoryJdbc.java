package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.adapter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.row.FriendshipsRow;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.mapper.FriendshipPersistenceMapper;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipsRepository;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipAlreadyExistsException;

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

    /** Reads a result set row into {@link FriendshipsRow}. */
    private static final RowMapper<FriendshipsRow> ROW_MAPPER =
            (resultSet, rowNum) -> new FriendshipsRow(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getObject("first_user_id", UUID.class),
                    resultSet.getObject("second_user_id", UUID.class),
                    resultSet.getObject("friendship_offer_id", UUID.class),
                    resultSet.getString("status"),
                    resultSet.getTimestamp("created_at").toInstant()
            );

    /** Executes the native SQL queries. */
    private final JdbcTemplate jdbcTemplate;

    /** Maps port requests/responses to rows and back. */
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
        var row = mapper.toRow(request);
        try {
            var saved = jdbcTemplate.queryForObject(
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
}
