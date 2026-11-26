package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.jpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;
import ru.job4j.socialmediaapi.support.PostgreSqlIntegrationTest;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@DisplayName("JPA-репозиторий заявок в друзья: таблица friendship_offer")
class FriendshipOfferJpaRepositoryTest extends PostgreSqlIntegrationTest {

    /** Проверяемый репозиторий. */
    @Autowired
    private FriendshipOfferJpaRepository repository;

    @Test
    @DisplayName("Создание заявки сохраняет участников, статус и время; JPA читает сохранённые данные")
    void whenInsertOfferThenJpaReadsStoredData() {
        var id = UUID.randomUUID();
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();
        var now = Instant.parse("2026-09-22T10:15:00Z");
        var expected = new FriendshipOfferEntity(
                id, fromUserId, toUserId, FriendshipOfferStatus.PENDING, now, now
        );

        assertThat(repository.create(id, fromUserId, toUserId, "PENDING", now)).isEqualTo(1);

        var stored = repository.findById(id).orElseThrow();
        assertThat(stored).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DisplayName("Повторная заявка для той же пары не вставляется: хранится первая заявка")
    void whenInsertDuplicateThenOriginalOfferRemains() {
        var firstId = UUID.randomUUID();
        var secondId = UUID.randomUUID();
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();
        var now = Instant.parse("2026-09-22T10:15:00Z");
        var expected = new FriendshipOfferEntity(
                firstId, fromUserId, toUserId, FriendshipOfferStatus.PENDING, now, now
        );

        assertThat(repository.create(firstId, fromUserId, toUserId, "PENDING", now)).isEqualTo(1);
        assertThat(repository.create(secondId, fromUserId, toUserId, "PENDING", now.plusSeconds(60))).isZero();

        var stored = repository.findById(firstId).orElseThrow();
        assertThat(stored).usingRecursiveComparison().isEqualTo(expected);
        assertThat(repository.findById(secondId)).isEmpty();
    }
}
