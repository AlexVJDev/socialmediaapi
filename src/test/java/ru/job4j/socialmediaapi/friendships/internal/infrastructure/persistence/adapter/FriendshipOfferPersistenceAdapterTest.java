package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipOfferRepository;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;
import ru.job4j.socialmediaapi.friendships.internal.application.exception.FriendshipOfferAlreadyExistsException;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FindFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendshipOfferCursor;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendshipOfferPageResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.UpdateFriendshipOfferStatusRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.UpdateFriendshipOfferStatusResponse;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@Testcontainers
@SpringBootTest
@DisplayName("Persistence-адаптер заявок в друзья: таблица friendship_offer")
class FriendshipOfferPersistenceAdapterTest {

    /** Throwaway PostgreSQL, started and stopped by the {@code @Testcontainers} extension. */
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17");

    /** Fixed decision time, later than the creation time of {@link #request}. */
    private static final Instant DECIDED_AT = Instant.parse("2026-10-06T10:20:00Z");

    /** Repository under test. */
    @Autowired
    private FriendshipOfferRepository repository;

    /** Direct access to the database to prepare and check data. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Runs code in a transaction, like the service does. */
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("Заявка с неизвестным статусом выбросит исключение из-за CHECK-ограничения на status")
    void whenCreateFriendshipOfferWithUnknownStatusThenThrowException() {
        var now = Timestamp.from(Instant.parse("2026-10-06T10:15:00Z"));

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO friendship_offer (
                    id, from_user_id, to_user_id,
                    status, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "UNKNOWN",
                now,
                now
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("createFriendshipOffer: новая заявка сохраняется и возвращается")
    void whenCreateOfferThenReturnStoredData() {
        var request = request(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
        );

        var result = repository.createFriendshipOffer(request);

        assertThat(result).isEqualTo(new CreateFriendshipOfferResponse(
                request.id(), request.fromUserId(), request.toUserId(),
                request.status(), request.createdAt(), request.updatedAt()
        ));
    }

    @Test
    @DisplayName("createFriendshipOffer: при существующей заявке from -> to (uq_friendship_offer_pair) "
            + "выбросит FriendshipOfferAlreadyExistsException, данные не меняются")
    void whenCreateDuplicateOfferThenThrowExceptionAndStoredOfferUnchanged() {
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();
        var existingId = UUID.randomUUID();
        repository.createFriendshipOffer(request(existingId, fromUserId, toUserId));

        assertThatThrownBy(() -> repository.createFriendshipOffer(request(
                UUID.randomUUID(), fromUserId, toUserId
        ))).isInstanceOf(FriendshipOfferAlreadyExistsException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
        assertThat(repository.findByPair(fromUserId, toUserId))
                .map(FindFriendshipOfferResponse::id)
                .contains(existingId);
    }

    @Test
    @DisplayName("createFriendshipOffer: встречная заявка to -> from при существующей from -> to "
            + "выбросит FriendshipOfferAlreadyExistsException, в БД одна заявка пары")
    void whenCreateCounterOfferThenThrowExceptionAndOnlyOneOfferOfPair() {
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();
        var existingId = UUID.randomUUID();
        repository.createFriendshipOffer(request(existingId, fromUserId, toUserId));

        assertThatThrownBy(() -> repository.createFriendshipOffer(request(
                UUID.randomUUID(), toUserId, fromUserId
        ))).isInstanceOf(FriendshipOfferAlreadyExistsException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM friendship_offer
                WHERE (from_user_id = ? AND to_user_id = ?) OR (from_user_id = ? AND to_user_id = ?)
                """, Integer.class, fromUserId, toUserId, toUserId, fromUserId)).isOne();
    }

    @Test
    @DisplayName("createFriendshipOffer: после дубликата транзакция вызывающего не прерывается, "
            + "заявку можно перечитать в ней же")
    void whenDuplicateInsideTransactionThenTransactionStillUsable() {
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();
        var existingId = UUID.randomUUID();
        repository.createFriendshipOffer(request(existingId, fromUserId, toUserId));

        var found = transactionTemplate.execute(status -> {
            assertThatThrownBy(() -> repository.createFriendshipOffer(request(
                    UUID.randomUUID(), fromUserId, toUserId
            ))).isInstanceOf(FriendshipOfferAlreadyExistsException.class);
            return repository.findByPair(fromUserId, toUserId);
        });

        assertThat(found)
                .map(FindFriendshipOfferResponse::id)
                .contains(existingId);
    }

    @Test
    @DisplayName("createFriendshipOffer: заявку самому себе отклонит CHECK в БД, адаптер её не переводит")
    void whenCreateOfferToSelfThenThrowException() {
        var userId = UUID.randomUUID();

        assertThatThrownBy(() -> repository.createFriendshipOffer(request(
                UUID.randomUUID(), userId, userId
        ))).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_friendship_offer_different_users");
    }

    @Test
    @DisplayName("Поиск заявки, которой нет, возвращает пустой результат")
    void whenFindByPairAndOfferNotExistsThenEmpty() {
        var result = repository.findByPair(UUID.randomUUID(), UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Поиск заявки пары в порядке from, to возвращает сохранённые в БД данные")
    void whenFindByPairThenReturnStoredData() {
        var id = UUID.randomUUID();
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();
        var created = repository.createFriendshipOffer(request(id, fromUserId, toUserId));

        var result = repository.findByPair(fromUserId, toUserId);

        assertThat(result).contains(new FindFriendshipOfferResponse(
                id,
                fromUserId,
                toUserId,
                FriendshipOfferStatus.PENDING,
                created.createdAt(),
                created.updatedAt()
        ));
    }

    @Test
    @DisplayName("Поиск заявки пары находит её при обратном порядке пользователей, направление заявки сохраняется")
    void whenFindByPairInOppositeOrderThenReturnOfferWithItsDirection() {
        var id = UUID.randomUUID();
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();
        repository.createFriendshipOffer(request(id, fromUserId, toUserId));

        var result = repository.findByPair(toUserId, fromUserId);

        assertThat(result).hasValueSatisfying(offer -> {
            assertThat(offer.id()).isEqualTo(id);
            assertThat(offer.fromUserId()).isEqualTo(fromUserId);
            assertThat(offer.toUserId()).isEqualTo(toUserId);
        });
    }

    @ParameterizedTest
    @EnumSource(value = FriendshipOfferStatus.class, names = {"ACCEPTED", "REJECTED"})
    @DisplayName("Поиск заявки возвращает статус, изменённый после создания")
    void whenFindByPairAfterStatusChangedThenReturnNewStatus(FriendshipOfferStatus status) {
        var id = UUID.randomUUID();
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();
        repository.createFriendshipOffer(request(id, fromUserId, toUserId));
        jdbcTemplate.update(
                "UPDATE friendship_offer SET status = ? WHERE id = ?",
                status.name(), id
        );

        var result = repository.findByPair(fromUserId, toUserId);

        assertThat(result)
                .map(FindFriendshipOfferResponse::status)
                .contains(status);
    }

    @ParameterizedTest
    @EnumSource(value = FriendshipOfferStatus.class, names = {"ACCEPTED", "REJECTED"})
    @DisplayName("updateStatusIfPending: PENDING-заявка получателя меняет статус и updated_at, остальное не меняется")
    void whenUpdatePendingOfferByRecipientThenUpdated(FriendshipOfferStatus decision) {
        var offer = repository.createFriendshipOffer(request(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));

        var result = repository.updateStatusIfPending(new UpdateFriendshipOfferStatusRequest(
                offer.id(), offer.toUserId(), decision, DECIDED_AT
        ));

        var expected = new UpdateFriendshipOfferStatusResponse(
                offer.id(), offer.fromUserId(), offer.toUserId(), decision, offer.createdAt(), DECIDED_AT
        );
        assertThat(result).contains(expected);
        assertThat(repository.findByIdAndRecipient(offer.id(), offer.toUserId()))
                .map(FindFriendshipOfferResponse::status)
                .contains(decision);
    }

    @Test
    @DisplayName("updateStatusIfPending: отправитель или посторонний не меняют заявку — пустой результат")
    void whenUpdateByNotRecipientThenEmptyAndUnchanged() {
        var offer = repository.createFriendshipOffer(request(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));

        for (var actorId : List.of(offer.fromUserId(), UUID.randomUUID())) {
            var result = repository.updateStatusIfPending(new UpdateFriendshipOfferStatusRequest(
                    offer.id(), actorId, FriendshipOfferStatus.ACCEPTED, DECIDED_AT
            ));

            assertThat(result).isEmpty();
        }
        assertThat(repository.findByIdAndRecipient(offer.id(), offer.toUserId()))
                .contains(new FindFriendshipOfferResponse(
                        offer.id(), offer.fromUserId(), offer.toUserId(),
                        FriendshipOfferStatus.PENDING, offer.createdAt(), offer.updatedAt()
                ));
    }

    @ParameterizedTest
    @EnumSource(value = FriendshipOfferStatus.class, names = {"ACCEPTED", "REJECTED"})
    @DisplayName("updateStatusIfPending: решённую заявку повторно не изменить — пустой результат, статус прежний")
    void whenUpdateDecidedOfferThenEmptyAndUnchanged(FriendshipOfferStatus decided) {
        var offer = repository.createFriendshipOffer(request(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
        repository.updateStatusIfPending(new UpdateFriendshipOfferStatusRequest(
                offer.id(), offer.toUserId(), decided, DECIDED_AT
        ));

        for (var decision : List.of(FriendshipOfferStatus.ACCEPTED, FriendshipOfferStatus.REJECTED)) {
            var result = repository.updateStatusIfPending(new UpdateFriendshipOfferStatusRequest(
                    offer.id(), offer.toUserId(), decision, DECIDED_AT.plusSeconds(60)
            ));

            assertThat(result).isEmpty();
        }
        assertThat(repository.findByIdAndRecipient(offer.id(), offer.toUserId()))
                .hasValueSatisfying(stored -> {
                    assertThat(stored.status()).isEqualTo(decided);
                    assertThat(stored.updatedAt()).isEqualTo(DECIDED_AT);
                });
    }

    @Test
    @DisplayName("updateStatusIfPending: несуществующая заявка — пустой результат")
    void whenUpdateMissingOfferThenEmpty() {
        var result = repository.updateStatusIfPending(new UpdateFriendshipOfferStatusRequest(
                UUID.randomUUID(), UUID.randomUUID(), FriendshipOfferStatus.ACCEPTED, DECIDED_AT
        ));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndRecipient: находит заявку получателя, но не отправителю, постороннему и по чужому id")
    void whenFindByIdAndRecipientThenOnlyRecipientFindsOffer() {
        var offer = repository.createFriendshipOffer(request(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));

        assertThat(repository.findByIdAndRecipient(offer.id(), offer.toUserId()))
                .map(FindFriendshipOfferResponse::id)
                .contains(offer.id());
        assertThat(repository.findByIdAndRecipient(offer.id(), offer.fromUserId())).isEmpty();
        assertThat(repository.findByIdAndRecipient(offer.id(), UUID.randomUUID())).isEmpty();
        assertThat(repository.findByIdAndRecipient(UUID.randomUUID(), offer.toUserId())).isEmpty();
    }

    @Test
    @DisplayName("findById: заявка читается в любом статусе со всеми полями")
    void whenFindByIdThenReturnOfferInAnyStatus() {
        var created = repository.createFriendshipOffer(
                request(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
        repository.updateStatusIfPending(new UpdateFriendshipOfferStatusRequest(
                created.id(), created.toUserId(), FriendshipOfferStatus.ACCEPTED, DECIDED_AT));

        var found = repository.findById(created.id());

        assertThat(found).contains(new FindFriendshipOfferResponse(
                created.id(), created.fromUserId(), created.toUserId(),
                FriendshipOfferStatus.ACCEPTED, created.createdAt(), DECIDED_AT
        ));
    }

    @Test
    @DisplayName("findById: несуществующая заявка — пустой результат")
    void whenFindByUnknownIdThenEmpty() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("findPage: первая страница — самые новые заявки, next указывает на последнюю заявку страницы")
    void whenFindFirstPageThenNewestOffers() {
        var newest = offerAt(newerThanAll());

        var page = repository.findPage(1);

        assertThat(page.offers()).containsExactly(newest);
        assertThat(page.next()).contains(cursorOf(newest));
    }

    @Test
    @DisplayName("Страницы по курсору: 5 заявок по 2 идут от новых к старым без повторов и пропусков")
    void whenPagedByCursorThenNewestFirstWithoutGaps() {
        var window = window();
        var offers = new ArrayList<FindFriendshipOfferResponse>();
        for (int i = 1; i <= 5; i++) {
            offers.add(offerAt(window.plusSeconds(i)));
        }
        var start = new FriendshipOfferCursor(window.plusSeconds(6), UUID.randomUUID());

        var first = repository.findPageAfter(start, 2);
        var second = repository.findPageAfter(first.next().orElseThrow(), 2);
        var third = repository.findPageAfter(second.next().orElseThrow(), 2);

        assertThat(first.offers()).containsExactly(offers.get(4), offers.get(3));
        assertThat(first.next()).contains(cursorOf(offers.get(3)));
        assertThat(second.offers()).containsExactly(offers.get(2), offers.get(1));
        assertThat(third.offers()).first().isEqualTo(offers.get(0));
    }

    @Test
    @DisplayName("Заявки с одинаковым created_at идут по id DESC (как сравнивает PostgreSQL), курсор не теряет ни одну")
    void whenSameCreatedAtThenOrderedByIdDescAcrossPages() {
        var createdAt = window();
        var offers = List.of(offerAt(createdAt), offerAt(createdAt), offerAt(createdAt));
        var expected = offers.stream()
                .sorted(Comparator.comparing((FindFriendshipOfferResponse o) -> o.id().toString()).reversed())
                .toList();
        var start = new FriendshipOfferCursor(createdAt.plusSeconds(1), UUID.randomUUID());

        var first = repository.findPageAfter(start, 2);
        var second = repository.findPageAfter(first.next().orElseThrow(), 2);

        assertThat(first.offers()).containsExactly(expected.get(0), expected.get(1));
        assertThat(second.offers()).first().isEqualTo(expected.get(2));
    }

    @Test
    @DisplayName("Конец списка: на последней странице next пуст, в том числе когда заявок ровно limit")
    void whenNoMoreOffersThenNextIsEmpty() {
        var oldest = offerAt(Instant.parse("1971-01-01T00:00:01Z"));
        var older = offerAt(Instant.parse("1971-01-01T00:00:00Z"));
        var start = new FriendshipOfferCursor(Instant.parse("1971-01-01T00:00:02Z"), UUID.randomUUID());

        var exactlyLimit = repository.findPageAfter(start, 2);
        var oneByOne = repository.findPageAfter(start, 1);

        assertThat(exactlyLimit).isEqualTo(new FriendshipOfferPageResponse(List.of(oldest, older), Optional.empty()));
        assertThat(oneByOne.offers()).containsExactly(oldest);
        assertThat(oneByOne.next()).contains(cursorOf(oldest));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, FriendshipOfferRepository.MAX_LIMIT + 1})
    @DisplayName("limit вне 1..MAX_LIMIT: IllegalArgumentException до запроса")
    void whenLimitOutOfRangeThenIllegalArgument(int limit) {
        var after = new FriendshipOfferCursor(Instant.now(), UUID.randomUUID());

        assertThatThrownBy(() -> repository.findPage(limit)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> repository.findPageAfter(after, limit)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("findPageAfter без курсора: NullPointerException")
    void whenAfterIsNullThenNullPointer() {
        assertThatThrownBy(() -> repository.findPageAfter(null, 1)).isInstanceOf(NullPointerException.class);
    }

    /**
     * @return start of a time range of one test, far in the future: no other offer falls into it
     */
    private static Instant window() {
        return Instant.parse("2900-01-01T00:00:00Z")
                .plusSeconds(ThreadLocalRandom.current().nextLong(1_000_000_000L) * 10);
    }

    /**
     * Time later than every stored offer, at least {@code 9000-01-01}: the offer created at it is
     * the newest one even if the test is run again on the same database (a fixed date would give
     * two offers with the same {@code created_at}, ordered by a random {@code id}).
     *
     * @return one second after the newest stored offer, but not earlier than {@code 9000-01-01}
     */
    private Instant newerThanAll() {
        var floor = Instant.parse("9000-01-01T00:00:00Z");
        return repository.findPage(1).offers().stream()
                .map(FindFriendshipOfferResponse::createdAt)
                .filter(floor::isBefore)
                .findFirst()
                .orElse(floor)
                .plusSeconds(1);
    }

    /**
     * Creates a {@code PENDING} offer of two new users through the port.
     *
     * @param createdAt time of creation (whole seconds: the column keeps microseconds)
     * @return the offer as read by {@code findById}
     */
    private FindFriendshipOfferResponse offerAt(Instant createdAt) {
        var created = repository.createFriendshipOffer(new CreateFriendshipOfferRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                FriendshipOfferStatus.PENDING, createdAt, createdAt
        ));
        return repository.findById(created.id()).orElseThrow();
    }

    private static FriendshipOfferCursor cursorOf(FindFriendshipOfferResponse offer) {
        return new FriendshipOfferCursor(offer.createdAt(), offer.id());
    }

    private CreateFriendshipOfferRequest request(
            UUID id,
            UUID fromUserId,
            UUID toUserId
    ) {
        var now = Instant.parse("2026-10-06T10:15:00Z");
        return new CreateFriendshipOfferRequest(
                id, fromUserId, toUserId, FriendshipOfferStatus.PENDING, now, now
        );
    }
}
