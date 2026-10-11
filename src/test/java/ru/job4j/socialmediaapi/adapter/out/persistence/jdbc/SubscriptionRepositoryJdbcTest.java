package ru.job4j.socialmediaapi.adapter.out.persistence.jdbc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ru.job4j.socialmediaapi.TestcontainersConfiguration;
import ru.job4j.socialmediaapi.application.port.out.SubscriptionRepository;
import ru.job4j.socialmediaapi.domain.subscription.exception.SubscriptionAlreadyExistsException;
import ru.job4j.socialmediaapi.domain.subscription.exception.SubscriptionToSelfException;
import ru.job4j.socialmediaapi.domain.subscription.exception.SubscriptionUserRequiredException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DisplayName("JDBC-репозиторий подписок: сохранение в таблицу subscriptions")
class SubscriptionRepositoryJdbcTest {

    private static final Instant NOW = Instant.parse("2026-10-06T10:15:00Z");

    @Autowired
    private SubscriptionRepository repository;

    @Test
    @DisplayName("Создание подписки возвращает сохранённые в БД данные")
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
    @DisplayName("Повторная подписка на того же пользователя выбросит SubscriptionAlreadyExistsException")
    void whenCreateDuplicateSubscriptionThenThrowException() {
        var followerId = UUID.randomUUID();
        var followedId = UUID.randomUUID();
        repository.createSubscription(
                request(UUID.randomUUID(), followerId, followedId)
        );

        assertThatThrownBy(() -> repository.createSubscription(
                request(UUID.randomUUID(), followerId, followedId)
        )).isInstanceOf(SubscriptionAlreadyExistsException.class);
    }

    @Test
    @DisplayName("Подписка на самого себя выбросит SubscriptionToSelfException")
    void whenCreateSubscriptionToSelfThenThrowException() {
        var userId = UUID.randomUUID();

        assertThatThrownBy(() -> repository.createSubscription(
                request(UUID.randomUUID(), userId, userId)
        )).isInstanceOf(SubscriptionToSelfException.class);
    }

    @Test
    @DisplayName("Подписка без followed_id выбросит SubscriptionUserRequiredException")
    void whenCreateSubscriptionToNullUserThenThrowException() {
        var followerId = UUID.randomUUID();

        assertThatThrownBy(() -> repository.createSubscription(
                request(UUID.randomUUID(), followerId, null)
        )).isInstanceOf(SubscriptionUserRequiredException.class)
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
