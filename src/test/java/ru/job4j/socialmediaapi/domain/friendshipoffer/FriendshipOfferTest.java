package ru.job4j.socialmediaapi.domain.friendshipoffer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferToSelfException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Агрегат заявки в друзья: инварианты при создании")
class FriendshipOfferTest {

    /** Fixed creation time. */
    private static final Instant NOW = Instant.parse("2026-10-14T10:15:00Z");

    @Test
    @DisplayName("Новая заявка создаётся в статусе PENDING с id и временем создания")
    void whenCreateThenPendingWithIdAndTime() {
        var fromUserId = UUID.randomUUID();
        var toUserId = UUID.randomUUID();

        var offer = FriendshipOffer.create(fromUserId, toUserId, NOW);

        assertThat(offer.id()).isNotNull();
        assertThat(offer.fromUserId()).isEqualTo(fromUserId);
        assertThat(offer.toUserId()).isEqualTo(toUserId);
        assertThat(offer.status()).isEqualTo(FriendshipOfferStatus.PENDING);
        assertThat(offer.createdAt()).isEqualTo(NOW);
        assertThat(offer.updatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Заявка самому себе выбросит FriendshipOfferToSelfException")
    void whenCreateToSelfThenThrowException() {
        var userId = UUID.randomUUID();

        assertThatThrownBy(() -> FriendshipOffer.create(userId, userId, NOW))
                .isInstanceOf(FriendshipOfferToSelfException.class);
    }

    @Test
    @DisplayName("Заявка без получателя выбросит NullPointerException")
    void whenCreateWithoutRecipientThenThrowException() {
        assertThatThrownBy(() -> FriendshipOffer.create(UUID.randomUUID(), null, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("toUserId");
    }

    @Test
    @DisplayName("Заявка без статуса выбросит NullPointerException")
    void whenStatusMissingThenThrowException() {
        assertThatThrownBy(() -> new FriendshipOffer(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, NOW, NOW
        )).isInstanceOf(NullPointerException.class)
                .hasMessage("status");
    }
}
