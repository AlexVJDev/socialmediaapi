package ru.job4j.socialmediaapi.friendships.internal.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Дружба: пара пользователей в каноническом порядке PostgreSQL")
class FriendshipTest {

    /** Fixed creation time. */
    private static final Instant NOW = Instant.parse("2026-10-14T10:20:00Z");

    /** Less than {@link #HIGH_BIT} for PostgreSQL, but greater for {@link UUID#compareTo}. */
    private static final UUID LOW = UUID.fromString("00000000-0000-0000-0000-000000000001");

    /** UUID with the high bit set: negative most significant {@code long}. */
    private static final UUID HIGH_BIT = UUID.fromString("80000000-0000-0000-0000-000000000000");

    @Test
    @DisplayName("between создаёт активную дружбу с id, заявкой и временем создания")
    void whenBetweenThenActiveFriendship() {
        var offerId = UUID.randomUUID();

        var friendship = Friendship.between(LOW, HIGH_BIT, offerId, NOW);

        assertThat(friendship.id()).isNotNull();
        assertThat(friendship.friendshipOfferId()).isEqualTo(offerId);
        assertThat(friendship.status()).isEqualTo(FriendshipStatus.ACTIVE);
        assertThat(friendship.createdAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("between(a, b) и between(b, a) дают одну и ту же пару")
    void whenBetweenInAnyOrderThenSamePair() {
        var userA = UUID.randomUUID();
        var userB = UUID.randomUUID();
        var offerId = UUID.randomUUID();

        var ab = Friendship.between(userA, userB, offerId, NOW);
        var ba = Friendship.between(userB, userA, offerId, NOW);

        assertThat(ba.firstUserId()).isEqualTo(ab.firstUserId());
        assertThat(ba.secondUserId()).isEqualTo(ab.secondUserId());
    }

    @Test
    @DisplayName("Порядок как в PostgreSQL (без знака), а не как в UUID.compareTo")
    void whenUuidOrdersDifferThenPostgresOrder() {
        assertThat(HIGH_BIT.compareTo(LOW)).as("UUID.compareTo считает HIGH_BIT меньшим").isNegative();

        var friendship = Friendship.between(HIGH_BIT, LOW, UUID.randomUUID(), NOW);

        assertThat(friendship.firstUserId()).isEqualTo(LOW);
        assertThat(friendship.secondUserId()).isEqualTo(HIGH_BIT);
    }

    @Test
    @DisplayName("Дружба пользователя с самим собой выбросит IllegalArgumentException")
    void whenSameUsersThenThrowException() {
        var userId = UUID.randomUUID();

        assertThatThrownBy(() -> Friendship.between(userId, userId, UUID.randomUUID(), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Пара не в каноническом порядке в конструкторе выбросит IllegalArgumentException")
    void whenNotCanonicalOrderThenThrowException() {
        assertThatThrownBy(() -> new Friendship(
                UUID.randomUUID(), HIGH_BIT, LOW, UUID.randomUUID(), FriendshipStatus.ACTIVE, NOW
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Без пользователя или заявки выбросит NullPointerException")
    void whenNullThenThrowException() {
        assertThatThrownBy(() -> Friendship.between(null, LOW, UUID.randomUUID(), NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("userA");
        assertThatThrownBy(() -> Friendship.between(HIGH_BIT, LOW, null, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("friendshipOfferId");
    }
}
