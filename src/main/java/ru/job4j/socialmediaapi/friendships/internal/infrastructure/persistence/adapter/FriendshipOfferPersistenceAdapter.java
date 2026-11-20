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
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendshipOfferCursor;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendshipOfferDirection;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendshipOfferPageResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.UpdateFriendshipOfferStatusRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.UpdateFriendshipOfferStatusResponse;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC-реализация порта {@link FriendshipOfferRepository}: нативный SQL по таблице
 * {@code friendship_offer}.
 */
@Repository
@RequiredArgsConstructor
public class FriendshipOfferPersistenceAdapter implements FriendshipOfferRepository {

    /** Вставляет заявку и возвращает сохранённую строку. */
    private static final String CREATE = """
            INSERT INTO friendship_offer (
                id, from_user_id, to_user_id,
                status, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id, from_user_id, to_user_id,
                      status, created_at, updated_at
            """;

    /** Выбирает заявку пары в любом направлении и блокирует её до конца транзакции. */
    private static final String FIND_BY_PAIR = """
            SELECT id, from_user_id, to_user_id,
                   status, created_at, updated_at
            FROM friendship_offer
            WHERE (from_user_id = ? AND to_user_id = ?)
               OR (from_user_id = ? AND to_user_id = ?)
            FOR UPDATE
            """;

    /**
     * Меняет статус {@code PENDING}-заявки данного получателя и возвращает сохранённую строку.
     * Одновременное решение ждёт блокировку строки, взятую первым, и после его коммита
     * перепроверяет {@code WHERE} на новой версии строки: {@code status = 'PENDING'} уже ложно,
     * поэтому ничего не обновляется.
     */
    private static final String UPDATE_STATUS_IF_PENDING = """
            UPDATE friendship_offer
            SET status = ?, updated_at = ?
            WHERE id = ? AND to_user_id = ? AND status = 'PENDING'
            RETURNING id, from_user_id, to_user_id,
                      status, created_at, updated_at
            """;

    /** Выбирает заявку получателя; без блокировки: строка читается только для выбора ошибки. */
    private static final String FIND_BY_ID_AND_RECIPIENT = """
            SELECT id, from_user_id, to_user_id,
                   status, created_at, updated_at
            FROM friendship_offer
            WHERE id = ? AND to_user_id = ?
            """;

    /** Выбирает заявку по id; без блокировки: строка только читается. */
    private static final String FIND_BY_ID = """
            SELECT id, from_user_id, to_user_id,
                   status, created_at, updated_at
            FROM friendship_offer
            WHERE id = ?
            """;

    /**
     * Первая страница входящих заявок пользователя. Параметры: получатель, статус, число читаемых строк.
     * Фильтр и порядок совпадают с индексом {@code idx_friendship_offer_incoming}.
     */
    private static final String FIND_INCOMING_FIRST_PAGE = """
            SELECT id, from_user_id, to_user_id,
                   status, created_at, updated_at
            FROM friendship_offer
            WHERE to_user_id = ? AND status = ?
            ORDER BY created_at DESC, id DESC
            LIMIT ?
            """;

    /**
     * Страница входящих заявок после курсора. Параметры: получатель, статус, {@code created_at} курсора,
     * {@code id} курсора, число читаемых строк. Сравнение строк становится условием индекса.
     */
    private static final String FIND_INCOMING_PAGE_AFTER = """
            SELECT id, from_user_id, to_user_id,
                   status, created_at, updated_at
            FROM friendship_offer
            WHERE to_user_id = ? AND status = ?
              AND (created_at, id) < (?, ?)
            ORDER BY created_at DESC, id DESC
            LIMIT ?
            """;

    /**
     * Первая страница исходящих заявок пользователя. Параметры: отправитель, статус, число читаемых строк.
     * Фильтр и порядок совпадают с индексом {@code idx_friendship_offer_outgoing}.
     */
    private static final String FIND_OUTGOING_FIRST_PAGE = """
            SELECT id, from_user_id, to_user_id,
                   status, created_at, updated_at
            FROM friendship_offer
            WHERE from_user_id = ? AND status = ?
            ORDER BY created_at DESC, id DESC
            LIMIT ?
            """;

    /**
     * Страница исходящих заявок после курсора. Параметры: отправитель, статус, {@code created_at} курсора,
     * {@code id} курсора, число читаемых строк.
     */
    private static final String FIND_OUTGOING_PAGE_AFTER = """
            SELECT id, from_user_id, to_user_id,
                   status, created_at, updated_at
            FROM friendship_offer
            WHERE from_user_id = ? AND status = ?
              AND (created_at, id) < (?, ?)
            ORDER BY created_at DESC, id DESC
            LIMIT ?
            """;

    /** Читает строку результата в {@link FriendshipOfferRow}. */
    private static final RowMapper<FriendshipOfferRow> ROW_MAPPER =
            (resultSet, rowNum) -> new FriendshipOfferRow(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getObject("from_user_id", UUID.class),
                    resultSet.getObject("to_user_id", UUID.class),
                    resultSet.getString("status"),
                    resultSet.getTimestamp("created_at").toInstant(),
                    resultSet.getTimestamp("updated_at").toInstant()
            );

    /** Выполняет нативные SQL-запросы. */
    private final JdbcTemplate jdbcTemplate;

    /** Преобразует запросы/ответы порта в строки и обратно. */
    private final FriendshipOfferPersistenceMapper mapper;

    /**
     * Выполняется во вложенной транзакции (savepoint внутри транзакции вызывающего):
     * нарушение уникальности прерывает всю транзакцию PostgreSQL, поэтому при дубликате
     * откатывается только вставка, и вызывающий ещё может прочитать существующую заявку.
     */
    @Override
    @Transactional(propagation = Propagation.NESTED)
    public CreateFriendshipOfferResponse createFriendshipOffer(
            CreateFriendshipOfferRequest request
    ) {
        FriendshipOfferRow row = mapper.toRow(request);
        try {
            FriendshipOfferRow saved = jdbcTemplate.queryForObject(
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

    @Override
    public Optional<FindFriendshipOfferResponse> findById(UUID offerId) {
        return jdbcTemplate.query(FIND_BY_ID, ROW_MAPPER, offerId)
                .stream()
                .findFirst()
                .map(mapper::toFindResponse);
    }

    @Override
    public FriendshipOfferPageResponse findPage(
            UUID userId,
            FriendshipOfferDirection direction,
            FriendshipOfferStatus status,
            int limit
    ) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(status, "status");
        checkLimit(limit);
        String sql = switch (direction) {
            case INCOMING -> FIND_INCOMING_FIRST_PAGE;
            case OUTGOING -> FIND_OUTGOING_FIRST_PAGE;
        };
        return findPage(limit, sql, userId, mapper.toStatus(status), limit + 1);
    }

    @Override
    public FriendshipOfferPageResponse findPageAfter(
            UUID userId,
            FriendshipOfferDirection direction,
            FriendshipOfferStatus status,
            FriendshipOfferCursor after,
            int limit
    ) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(after, "after");
        checkLimit(limit);
        String sql = switch (direction) {
            case INCOMING -> FIND_INCOMING_PAGE_AFTER;
            case OUTGOING -> FIND_OUTGOING_PAGE_AFTER;
        };
        return findPage(limit, sql, userId, mapper.toStatus(status),
                Timestamp.from(after.createdAt()), after.id(), limit + 1);
    }

    /**
     * Читает до {@code limit + 1} строк: лишняя строка не возвращается,
     * она лишь означает, что есть следующая страница.
     *
     * @param limit максимальное число заявок на странице
     * @param sql   запрос страницы
     * @param args  параметры запроса
     * @return страница с курсором следующей, если она есть
     */
    private FriendshipOfferPageResponse findPage(int limit, String sql, Object... args) {
        List<FriendshipOfferRow> rows = jdbcTemplate.query(sql, ROW_MAPPER, args);
        boolean hasNext = rows.size() > limit;
        List<FriendshipOfferRow> pageRows = hasNext ? rows.subList(0, limit) : rows;
        Optional<FriendshipOfferCursor> next = hasNext
                ? Optional.of(mapper.toCursor(pageRows.getLast()))
                : Optional.empty();
        return new FriendshipOfferPageResponse(
                pageRows.stream().map(mapper::toFindResponse).toList(),
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
