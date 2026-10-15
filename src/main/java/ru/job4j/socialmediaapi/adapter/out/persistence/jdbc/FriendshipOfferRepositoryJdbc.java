package ru.job4j.socialmediaapi.adapter.out.persistence.jdbc;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.adapter.out.persistence.row.FriendshipOfferRow;
import ru.job4j.socialmediaapi.adapter.out.persistence.mapper.FriendshipOfferPersistenceMapper;
import ru.job4j.socialmediaapi.application.port.out.FriendshipOfferRepository;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferAlreadyExistsException;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Repository
public class FriendshipOfferRepositoryJdbc implements FriendshipOfferRepository {

    /** Inserts an offer and returns the stored row. */
    private static final String CREATE = """
            INSERT INTO friendship_offer (
                id, from_user_id, to_user_id,
                status, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id, from_user_id, to_user_id,
                      status, created_at, updated_at
            """;

    /** Selects the offer for the pair and locks it until the end of the transaction. */
    private static final String FIND_BY_USERS = """
            SELECT id, from_user_id, to_user_id,
                   status, created_at, updated_at
            FROM friendship_offer
            WHERE from_user_id = ? AND to_user_id = ?
            FOR UPDATE
            """;

    /** Reads a result set row into {@link FriendshipOfferRow}. */
    private static final RowMapper<FriendshipOfferRow> ROW_MAPPER =
            (resultSet, rowNum) -> new FriendshipOfferRow(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getObject("from_user_id", UUID.class),
                    resultSet.getObject("to_user_id", UUID.class),
                    resultSet.getString("status"),
                    resultSet.getTimestamp("created_at").toInstant(),
                    resultSet.getTimestamp("updated_at").toInstant()
            );

    /** Executes the native SQL queries. */
    private final JdbcTemplate jdbcTemplate;

    /** Maps port requests/responses to rows and back. */
    private final FriendshipOfferPersistenceMapper mapper;

    public FriendshipOfferRepositoryJdbc(
            JdbcTemplate jdbcTemplate,
            FriendshipOfferPersistenceMapper mapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    /**
     * Runs in a nested transaction (a savepoint inside the caller's one):
     * a unique violation aborts the whole PostgreSQL transaction, so on a duplicate
     * only the insert is rolled back and the caller can still read the existing offer.
     */
    @Override
    @Transactional(propagation = Propagation.NESTED)
    public CreateFriendshipOfferResponse createFriendshipOffer(
            CreateFriendshipOfferRequest request
    ) {
        var row = mapper.toRow(request);
        try {
            var saved = jdbcTemplate.queryForObject(
                    CREATE,
                    ROW_MAPPER,
                    row.id(),
                    row.fromUserId(),
                    row.toUserId(),
                    row.status(),
                    Timestamp.from(row.createdAt()),
                    Timestamp.from(row.updatedAt())
            );
            return mapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            if ("uq_friendship_offer_users".equals(PostgresErrors.constraint(ex))) {
                throw new FriendshipOfferAlreadyExistsException(request.fromUserId(), request.toUserId(), ex);
            }
            throw ex;
        }
    }

    @Override
    public Optional<FindFriendshipOfferResponse> findByUsers(UUID fromUserId, UUID toUserId) {
        return jdbcTemplate.query(FIND_BY_USERS, ROW_MAPPER, fromUserId, toUserId)
                .stream()
                .findFirst()
                .map(mapper::toFindResponse);
    }
}
