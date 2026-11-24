package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.jpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Import;
import ru.job4j.socialmediaapi.TestcontainersConfiguration;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.Friendship;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@Import(TestcontainersConfiguration.class)
@DisplayName("JPA-репозиторий дружбы: таблица friendships")
class FriendshipJpaRepositoryTest {

    /** Проверяемый репозиторий. */
    @Autowired
    private FriendshipJpaRepository repository;

    /** Заявки: у дружбы внешний ключ на принятую заявку. */
    @Autowired
    private FriendshipOfferJpaRepository offers;

    @Test
    @DisplayName("Друг находится с любой стороны канонической пары: каждый участник видит другого")
    void whenFriendshipThenEachUserFindsTheOtherOnAnySide() {
        var now = Instant.parse("2026-09-22T10:15:00Z");
        var offerId = UUID.randomUUID();
        // Канонический порядок пары — как в PostgreSQL (UUID.compareTo сравнивает со знаком и даёт другой порядок).
        var friendship = Friendship.between(UUID.randomUUID(), UUID.randomUUID(), offerId, now);
        var first = friendship.firstUserId();
        var second = friendship.secondUserId();
        var friendshipId = friendship.id();
        offers.create(offerId, first, second, "ACCEPTED", now);

        assertThat(repository.create(friendshipId, first, second, offerId, "ACTIVE", now)).isEqualTo(1);

        assertThat(repository.findFriendsFirstPage(first, 10))
                .singleElement()
                .satisfies(view -> {
                    assertThat(view.getId()).isEqualTo(friendshipId);
                    assertThat(view.getFriendId()).isEqualTo(second);
                    assertThat(view.getCreatedAt()).isEqualTo(now);
                });
        assertThat(repository.findFriendsFirstPage(second, 10))
                .singleElement()
                .satisfies(view -> assertThat(view.getFriendId()).isEqualTo(first));
    }
}
