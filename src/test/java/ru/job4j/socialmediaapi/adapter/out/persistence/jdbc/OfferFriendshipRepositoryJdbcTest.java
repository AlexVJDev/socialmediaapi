package ru.job4j.socialmediaapi.adapter.out.persistence.jdbc;

import org.junit.jupiter.api.Test;
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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@Testcontainers
@SpringBootTest
class OfferFriendshipRepositoryJdbcTest {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private OfferFriendshipRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void whenCreateOfferFriendshipThenReturnStoredData() {
        var id = UUID.randomUUID();
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();
        var now = Instant.parse("2026-01-06T10:15:00Z");
        var request = new OfferFriendshipRepository.CreateOfferFriendshipRequest(
                id,
                fromUserId,
                toUserId,
                OfferFriendshipStatus.PENDING,
                now,
                now
        );

        var result = repository.createOfferFriendship(request);

        var expected = new OfferFriendshipRepository
                .CreateOfferFriendshipResponse(
                id,
                fromUserId,
                toUserId,
                OfferFriendshipStatus.PENDING,
                now,
                now
        );
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void whenCreateDuplicateOfferFriendshipThenThrowException() {
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();
        repository.createOfferFriendship(request(
                UUID.randomUUID(), fromUserId, toUserId, OfferFriendshipStatus.PENDING
        ));

        assertThatThrownBy(() -> repository.createOfferFriendship(request(
                UUID.randomUUID(), fromUserId, toUserId, OfferFriendshipStatus.PENDING
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void whenCreateOfferFriendshipToSelfThenThrowException() {
        var userId = UUID.randomUUID();

        assertThatThrownBy(() -> repository.createOfferFriendship(request(
                UUID.randomUUID(), userId, userId, OfferFriendshipStatus.PENDING
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
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

    private OfferFriendshipRepository.CreateOfferFriendshipRequest request(
            UUID id,
            UUID fromUserId,
            UUID toUserId,
            OfferFriendshipStatus status
    ) {
        var now = Instant.parse("2026-10-06T10:15:00Z");
        return new OfferFriendshipRepository.CreateOfferFriendshipRequest(
                id, fromUserId, toUserId, status, now, now
        );
    }
}
