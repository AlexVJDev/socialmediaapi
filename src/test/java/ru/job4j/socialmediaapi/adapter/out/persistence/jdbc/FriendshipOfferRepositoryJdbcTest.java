package ru.job4j.socialmediaapi.adapter.out.persistence.jdbc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.job4j.socialmediaapi.application.port.out.FriendshipOfferRepository;
import ru.job4j.socialmediaapi.domain.friendshipoffer.FriendshipOfferStatus;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferToSelfException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@Testcontainers
@SpringBootTest
@DisplayName("JDBC-репозиторий заявок в друзья: сохранение в таблицу friendship_offer")
class FriendshipOfferRepositoryJdbcTest {

    /** Throwaway PostgreSQL, started and stopped by the {@code @Testcontainers} extension. */
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17");

    /** Repository under test. */
    @Autowired
    private FriendshipOfferRepository repository;

    /** Direct access to the database to prepare and check data. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    @DisplayName("createFriendshipOfferIfAbsent: новая заявка сохраняется и возвращается")
    void whenCreateIfAbsentAndOfferNotExistsThenReturnStoredData() {
        var request = request(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
        );

        var result = repository.createFriendshipOfferIfAbsent(request);

        assertThat(result).contains(new FriendshipOfferRepository.CreateFriendshipOfferResponse(
                request.id(), request.fromUserId(), request.toUserId(),
                request.status(), request.createdAt(), request.updatedAt()
        ));
    }

    @Test
    @DisplayName("createFriendshipOfferIfAbsent: при существующей заявке from -> to "
            + "пустой результат, данные не меняются")
    void whenCreateIfAbsentAndOfferExistsThenEmptyAndStoredOfferUnchanged() {
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();
        var existingId = UUID.randomUUID();
        repository.createFriendshipOfferIfAbsent(request(existingId, fromUserId, toUserId));

        var result = repository.createFriendshipOfferIfAbsent(request(
                UUID.randomUUID(), fromUserId, toUserId
        ));

        assertThat(result).isEmpty();
        assertThat(repository.findByUsers(fromUserId, toUserId))
                .map(FriendshipOfferRepository.FindFriendshipOfferResponse::id)
                .contains(existingId);
    }

    @Test
    @DisplayName("createFriendshipOfferIfAbsent: заявка самому себе выбросит FriendshipOfferToSelfException")
    void whenCreateIfAbsentToSelfThenThrowException() {
        var userId = UUID.randomUUID();

        assertThatThrownBy(() -> repository.createFriendshipOfferIfAbsent(request(
                UUID.randomUUID(), userId, userId
        ))).isInstanceOf(FriendshipOfferToSelfException.class);
    }

    @Test
    @DisplayName("Поиск заявки, которой нет, возвращает пустой результат")
    void whenFindByUsersAndOfferNotExistsThenEmpty() {
        var result = repository.findByUsers(UUID.randomUUID(), UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Поиск заявки from -> to возвращает сохранённые в БД данные")
    void whenFindByUsersThenReturnStoredData() {
        var id = UUID.randomUUID();
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();
        var created = repository.createFriendshipOfferIfAbsent(request(id, fromUserId, toUserId)).orElseThrow();

        var result = repository.findByUsers(fromUserId, toUserId);

        assertThat(result).contains(new FriendshipOfferRepository.FindFriendshipOfferResponse(
                id,
                fromUserId,
                toUserId,
                FriendshipOfferStatus.PENDING,
                created.createdAt(),
                created.updatedAt()
        ));
    }

    @Test
    @DisplayName("Поиск заявки from -> to не находит встречную заявку to -> from")
    void whenFindByUsersAndOnlyOppositeOfferExistsThenEmpty() {
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();
        repository.createFriendshipOfferIfAbsent(request(
                UUID.randomUUID(), toUserId, fromUserId
        ));

        var result = repository.findByUsers(fromUserId, toUserId);

        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = FriendshipOfferStatus.class, names = {"ACCEPTED", "REJECTED"})
    @DisplayName("Поиск заявки возвращает статус, изменённый после создания")
    void whenFindByUsersAfterStatusChangedThenReturnNewStatus(FriendshipOfferStatus status) {
        var id = UUID.randomUUID();
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();
        repository.createFriendshipOfferIfAbsent(request(id, fromUserId, toUserId));
        jdbcTemplate.update(
                "UPDATE friendship_offer SET status = ? WHERE id = ?",
                status.name(), id
        );

        var result = repository.findByUsers(fromUserId, toUserId);

        assertThat(result)
                .map(FriendshipOfferRepository.FindFriendshipOfferResponse::status)
                .contains(status);
    }

    private FriendshipOfferRepository.CreateFriendshipOfferRequest request(
            UUID id,
            UUID fromUserId,
            UUID toUserId
    ) {
        var now = Instant.parse("2026-10-06T10:15:00Z");
        return new FriendshipOfferRepository.CreateFriendshipOfferRequest(
                id, fromUserId, toUserId, FriendshipOfferStatus.PENDING, now, now
        );
    }
}
