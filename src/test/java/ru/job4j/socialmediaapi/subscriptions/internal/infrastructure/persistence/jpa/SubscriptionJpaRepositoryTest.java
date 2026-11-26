package ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.persistence.jpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import ru.job4j.socialmediaapi.support.PostgreSqlIntegrationTest;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@DisplayName("JPA-репозиторий подписок: таблица subscriptions")
class SubscriptionJpaRepositoryTest extends PostgreSqlIntegrationTest {

    /** Проверяемый репозиторий. */
    @Autowired
    private SubscriptionJpaRepository repository;

    @Test
    @DisplayName("Подписка A → B: A — подписчик B, но не наоборот; B — в подписках A")
    void whenSubscribeThenFollowersRespectDirection() {
        var a = UUID.randomUUID();
        var b = UUID.randomUUID();
        var id = UUID.randomUUID();
        var now = Instant.parse("2026-09-22T10:15:00Z");
        var expected = new SubscriptionEntity(id, a, b, now);

        assertThat(repository.create(id, a, b, now)).isEqualTo(1);

        assertThat(repository.findByFollowedIdOrderByCreatedAtDescIdDesc(b))
                .singleElement()
                .usingRecursiveComparison()
                .isEqualTo(expected);
        assertThat(repository.findByFollowedIdOrderByCreatedAtDescIdDesc(a)).isEmpty();
        assertThat(repository.findByFollowerIdOrderByCreatedAtDescIdDesc(a))
                .singleElement()
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }
}
