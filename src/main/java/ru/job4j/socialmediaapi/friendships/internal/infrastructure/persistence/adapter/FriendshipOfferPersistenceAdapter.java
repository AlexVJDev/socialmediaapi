package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.row.FriendshipOfferRow;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.mapper.FriendshipOfferPersistenceMapper;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipOfferRepository;
import ru.job4j.socialmediaapi.friendships.internal.application.exception.FriendshipOfferAlreadyExistsException;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FindFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.UpdateFriendshipOfferStatusRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.UpdateFriendshipOfferStatusResponse;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC implementation of the {@link FriendshipOfferRepository} port: native SQL over the
 * {@code friendship_offer} table.
 */
@Repository
@RequiredArgsConstructor
public class FriendshipOfferPersistenceAdapter implements FriendshipOfferRepository {

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

    /** Selects the offer of the pair in either direction and locks it until the end of the transaction. */
    private static final String FIND_BY_PAIR = """
            SELECT id, from_user_id, to_user_id,
                   status, created_at, updated_at
            FROM friendship_offer
            WHERE (from_user_id = ? AND to_user_id = ?)
               OR (from_user_id = ? AND to_user_id = ?)
            FOR UPDATE
            """;

    /**
     * Changes the status of a {@code PENDING} offer of the given recipient and returns the stored row.
     * A concurrent decision waits for the row lock of the first one and, after its commit,
     * re-checks the {@code WHERE} on the new row version: {@code status = 'PENDING'} is false
     * by then, so it updates nothing.
     */
    private static final String UPDATE_STATUS_IF_PENDING = """
            UPDATE friendship_offer
            SET status = ?, updated_at = ?
            WHERE id = ? AND to_user_id = ? AND status = 'PENDING'
            RETURNING id, from_user_id, to_user_id,
                      status, created_at, updated_at
            """;

    /** Selects the offer of the recipient; no lock: the row is only read to choose the error. */
    private static final String FIND_BY_ID_AND_RECIPIENT = """
            SELECT id, from_user_id, to_user_id,
                   status, created_at, updated_at
            FROM friendship_offer
            WHERE id = ? AND to_user_id = ?
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
            if ("uq_friendship_offer_pair".equals(PostgresErrors.constraint(ex))) {
                throw new FriendshipOfferAlreadyExistsException(request.fromUserId(), request.toUserId(), ex);
            }
            throw ex;
        }
    }

    @Override
    public Optional<FindFriendshipOfferResponse> findByPair(UUID userA, UUID userB) {
        return jdbcTemplate.query(FIND_BY_PAIR, ROW_MAPPER, userA, userB, userB, userA)
                .stream()
                .findFirst()
                .map(mapper::toFindResponse);
    }

    @Override
    public Optional<UpdateFriendshipOfferStatusResponse> updateStatusIfPending(
            UpdateFriendshipOfferStatusRequest request
    ) {
        return jdbcTemplate.query(
                        UPDATE_STATUS_IF_PENDING,
                        ROW_MAPPER,
                        mapper.toStatus(request.status()),
                        Timestamp.from(request.updatedAt()),
                        request.offerId(),
                        request.actorId()
                )
                .stream()
                .findFirst()
                .map(mapper::toUpdateResponse);
    }

    @Override
    public Optional<FindFriendshipOfferResponse> findByIdAndRecipient(UUID offerId, UUID recipientId) {
        return jdbcTemplate.query(FIND_BY_ID_AND_RECIPIENT, ROW_MAPPER, offerId, recipientId)
                .stream()
                .findFirst()
                .map(mapper::toFindResponse);
    }
}
