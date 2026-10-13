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
import ru.job4j.socialmediaapi.application.port.out.OfferFriendshipRepository;
import ru.job4j.socialmediaapi.domain.offerfriendship.OfferFriendshipStatus;
import ru.job4j.socialmediaapi.domain.offerfriendship.exception.OfferFriendshipToSelfException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@Testcontainers
@SpringBootTest
@DisplayName("JDBC-репозиторий заявок в друзья: сохранение в таблицу offer_friendships")
class FriendshipOfferRepositoryJdbcTest {

    /** Throwaway PostgreSQL, started and stopped by the {@code @Testcontainers} extension. */
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17");

    /** Repository under test. */
    @Autowired
    private OfferFriendshipRepository repository;

    /** Direct access to the database to prepare and check data. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Заявка с неизвестным статусом выбросит исключение из-за CHECK-ограничения на status")
    void whenCreateOfferFriendshipWithUnknownStatusThenThrowException() {
        var now = Timestamp.from(Instant.parse("2026-10-06T10:15:00Z"));

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO offer_friendships (
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
    @DisplayName("createOfferFriendshipIfAbsent: новая заявка сохраняется и возвращается")
    void whenCreateIfAbsentAndOfferNotExistsThenReturnStoredData() {
        var request = request(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
        );

        var result = repository.createOfferFriendshipIfAbsent(request);

        assertThat(result).contains(new OfferFriendshipRepository.CreateOfferFriendshipResponse(
                request.id(), request.fromUserId(), request.toUserId(),
                request.status(), request.createdAt(), request.updatedAt()
        ));
    }

    @Test
    @DisplayName("createOfferFriendshipIfAbsent: при существующей заявке from -> to "
            + "пустой результат, данные не меняются")
    void whenCreateIfAbsentAndOfferExistsThenEmptyAndStoredOfferUnchanged() {
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();
        var existingId = UUID.randomUUID();
        repository.createOfferFriendshipIfAbsent(request(existingId, fromUserId, toUserId));

        var result = repository.createOfferFriendshipIfAbsent(request(
                UUID.randomUUID(), fromUserId, toUserId
        ));

        assertThat(result).isEmpty();
        assertThat(repository.findByUsers(fromUserId, toUserId))
                .map(OfferFriendshipRepository.FindOfferFriendshipResponse::id)
                .contains(existingId);
    }

    @Test
    @DisplayName("createOfferFriendshipIfAbsent: заявка самому себе выбросит OfferFriendshipToSelfException")
    void whenCreateIfAbsentToSelfThenThrowException() {
        var userId = UUID.randomUUID();

        assertThatThrownBy(() -> repository.createOfferFriendshipIfAbsent(request(
                UUID.randomUUID(), userId, userId
        ))).isInstanceOf(OfferFriendshipToSelfException.class);
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
        var created = repository.createOfferFriendshipIfAbsent(request(id, fromUserId, toUserId)).orElseThrow();

        var result = repository.findByUsers(fromUserId, toUserId);

        assertThat(result).contains(new OfferFriendshipRepository.FindOfferFriendshipResponse(
                id,
                fromUserId,
                toUserId,
                OfferFriendshipStatus.PENDING,
                created.createdAt(),
                created.updatedAt()
        ));
    }

    @Test
    @DisplayName("Поиск заявки from -> to не находит встречную заявку to -> from")
    void whenFindByUsersAndOnlyOppositeOfferExistsThenEmpty() {
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();
        repository.createOfferFriendshipIfAbsent(request(
                UUID.randomUUID(), toUserId, fromUserId
        ));

        var result = repository.findByUsers(fromUserId, toUserId);

        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = OfferFriendshipStatus.class, names = {"ACCEPTED", "REJECTED"})
    @DisplayName("Поиск заявки возвращает статус, изменённый после создания")
    void whenFindByUsersAfterStatusChangedThenReturnNewStatus(OfferFriendshipStatus status) {
        var id = UUID.randomUUID();
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();
        repository.createOfferFriendshipIfAbsent(request(id, fromUserId, toUserId));
        jdbcTemplate.update(
                "UPDATE offer_friendships SET status = ? WHERE id = ?",
                status.name(), id
        );

        var result = repository.findByUsers(fromUserId, toUserId);

        assertThat(result)
                .map(OfferFriendshipRepository.FindOfferFriendshipResponse::status)
                .contains(status);
    }

    private OfferFriendshipRepository.CreateOfferFriendshipRequest request(
            UUID id,
            UUID fromUserId,
            UUID toUserId
    ) {
        var now = Instant.parse("2026-10-06T10:15:00Z");
        return new OfferFriendshipRepository.CreateOfferFriendshipRequest(
                id, fromUserId, toUserId, OfferFriendshipStatus.PENDING, now, now
        );
    }
}
