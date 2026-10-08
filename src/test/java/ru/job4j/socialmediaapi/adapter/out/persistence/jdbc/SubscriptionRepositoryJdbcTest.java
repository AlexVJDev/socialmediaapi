package ru.job4j.socialmediaapi.adapter.out.persistence.jdbc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import ru.job4j.socialmediaapi.TestcontainersConfiguration;
import ru.job4j.socialmediaapi.application.port.out.SubscriptionRepository;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SubscriptionRepositoryJdbcTest {

    private static final Instant NOW = Instant.parse("2026-10-06T10:15:00Z");

    @Autowired
    private SubscriptionRepository repository;

    @Test
    void whenCreateSubscriptionThenReturnStoredData() {
        var id = UUID.randomUUID();
        var followerId = UUID.randomUUID();
        var followedId = UUID.randomUUID();

        var result = repository.createSubscription(
                request(id, followerId, followedId)
        );

        var expected = new SubscriptionRepository.CreateSubscriptionResponse(
                id, followerId, followedId, NOW
        );
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void whenCreateDuplicateSubscriptionThenThrowException() {
        var followerId = UUID.randomUUID();
        var followedId = UUID.randomUUID();
        repository.createSubscription(
                request(UUID.randomUUID(), followerId, followedId)
        );

        assertThatThrownBy(() -> repository.createSubscription(
                request(UUID.randomUUID(), followerId, followedId)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void whenCreateSubscriptionToSelfThenThrowException() {
        var userId = UUID.randomUUID();

        assertThatThrownBy(() -> repository.createSubscription(
                request(UUID.randomUUID(), userId, userId)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void whenCreateSubscriptionToNullUserThenThrowException() {
        var followerId = UUID.randomUUID();

        assertThatThrownBy(() -> repository.createSubscription(
                request(UUID.randomUUID(), followerId, null)
        )).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("followed_id");
    }

    private static SubscriptionRepository.CreateSubscriptionRequest request(
            UUID id,
            UUID followerId,
            UUID followedId
    ) {
        return new SubscriptionRepository.CreateSubscriptionRequest(
                id, followerId, followedId, NOW
        );
    }
}
