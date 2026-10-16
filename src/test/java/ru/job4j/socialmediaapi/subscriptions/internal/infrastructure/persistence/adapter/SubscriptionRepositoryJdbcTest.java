package ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.persistence.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.job4j.socialmediaapi.TestcontainersConfiguration;
import ru.job4j.socialmediaapi.subscriptions.internal.application.port.SubscriptionRepository;
import ru.job4j.socialmediaapi.subscriptions.internal.domain.exception.SubscriptionAlreadyExistsException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DisplayName("JDBC-репозиторий подписок: сохранение в таблицу subscriptions")
class SubscriptionRepositoryJdbcTest {

    /** Fixed time used for created rows. */
    private static final Instant NOW = Instant.parse("2026-10-06T10:15:00Z");

    /** Repository under test. */
    @Autowired
    private SubscriptionRepository repository;

    /** Direct access to the table to count rows. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    @DisplayName("Подписку на самого себя отклонит CHECK в БД, адаптер её не переводит")
    void whenCreateSubscriptionToSelfThenThrowException() {
        var userId = UUID.randomUUID();

        assertThatThrownBy(() -> repository.createSubscription(
                request(UUID.randomUUID(), userId, userId)
        )).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_subscriptions_different_users");
    }

    @Test
    @DisplayName("Подписку без followed_id отклонит NOT NULL в БД, адаптер её не переводит")
    void whenCreateSubscriptionToNullUserThenThrowException() {
        var followerId = UUID.randomUUID();

        assertThatThrownBy(() -> repository.createSubscription(
                request(UUID.randomUUID(), followerId, null)
        )).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("followed_id");
    }

    @Test
    @DisplayName("createSubscriptionIfAbsent: новая подписка сохраняется и возвращается")
    void whenCreateSubscriptionIfAbsentThenReturnStoredData() {
        var id = UUID.randomUUID();
        var followerId = UUID.randomUUID();
        var followedId = UUID.randomUUID();

        var result = repository.createSubscriptionIfAbsent(request(id, followerId, followedId));

        assertThat(result).contains(new SubscriptionRepository.CreateSubscriptionResponse(
                id, followerId, followedId, NOW
        ));
    }

    @Test
    @DisplayName("createSubscriptionIfAbsent: при существующей подписке возвращает пустой результат, "
            + "вторая строка не создаётся")
    void whenCreateSubscriptionIfAbsentForExistingThenEmptyAndNoDuplicate() {
        var followerId = UUID.randomUUID();
        var followedId = UUID.randomUUID();
        repository.createSubscriptionIfAbsent(request(UUID.randomUUID(), followerId, followedId));

        var result = repository.createSubscriptionIfAbsent(request(UUID.randomUUID(), followerId, followedId));

        assertThat(result).isEmpty();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM subscriptions WHERE follower_id = ? AND followed_id = ?",
                Integer.class, followerId, followedId
        )).isEqualTo(1);
    }

    @Test
    @DisplayName("existsSubscription: существующая подписка follower -> followed возвращает true")
    void whenSubscriptionExistsThenExistsReturnsTrue() {
        var followerId = UUID.randomUUID();
        var followedId = UUID.randomUUID();
        repository.createSubscription(request(UUID.randomUUID(), followerId, followedId));

        assertThat(repository.existsSubscription(followerId, followedId)).isTrue();
    }

    @Test
    @DisplayName("existsSubscription: без подписки, в том числе при наличии встречной, возвращает false")
    void whenOnlyOppositeSubscriptionExistsThenExistsReturnsFalse() {
        var followerId = UUID.randomUUID();
        var followedId = UUID.randomUUID();
        repository.createSubscription(request(UUID.randomUUID(), followedId, followerId));

        assertThat(repository.existsSubscription(followerId, followedId)).isFalse();
    }

    @Test
    @DisplayName("deleteSubscription: существующая подписка удаляется, возвращается true")
    void whenDeleteExistingSubscriptionThenTrueAndRowRemoved() {
        var followerId = UUID.randomUUID();
        var followedId = UUID.randomUUID();
        repository.createSubscription(request(UUID.randomUUID(), followerId, followedId));

        var deleted = repository.deleteSubscription(followerId, followedId);

        assertThat(deleted).isTrue();
        assertThat(repository.existsSubscription(followerId, followedId)).isFalse();
    }

    @Test
    @DisplayName("deleteSubscription: подписки нет — возвращается false")
    void whenDeleteMissingSubscriptionThenFalse() {
        assertThat(repository.deleteSubscription(UUID.randomUUID(), UUID.randomUUID())).isFalse();
    }

    @Test
    @DisplayName("deleteSubscription: удаляется только направление follower -> followed, встречная подписка остаётся")
    void whenDeleteSubscriptionThenOppositeSubscriptionKept() {
        var followerId = UUID.randomUUID();
        var followedId = UUID.randomUUID();
        repository.createSubscription(request(UUID.randomUUID(), followerId, followedId));
        repository.createSubscription(request(UUID.randomUUID(), followedId, followerId));

        repository.deleteSubscription(followerId, followedId);

        assertThat(repository.existsSubscription(followerId, followedId)).isFalse();
        assertThat(repository.existsSubscription(followedId, followerId)).isTrue();
    }

    @Test
    @DisplayName("deleteSubscription: после отписки можно подписаться снова")
    void whenSubscribeAgainAfterDeleteThenCreated() {
        var followerId = UUID.randomUUID();
        var followedId = UUID.randomUUID();
        repository.createSubscription(request(UUID.randomUUID(), followerId, followedId));
        repository.deleteSubscription(followerId, followedId);
        var newId = UUID.randomUUID();

        var result = repository.createSubscription(request(newId, followerId, followedId));

        assertThat(result.id()).isEqualTo(newId);
        assertThat(repository.existsSubscription(followerId, followedId)).isTrue();
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
