package ru.job4j.socialmediaapi.friendships.internal.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyAcceptedException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyRejectedException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferToSelfException;

import java.time.Instant;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Агрегат заявки в друзья: инварианты при создании, принятие и отклонение")
class FriendshipOfferTest {

    /** Fixed creation time. */
    private static final Instant NOW = Instant.parse("2026-10-14T10:15:00Z");

    /** Fixed decision time, later than {@link #NOW}. */
    private static final Instant DECIDED_AT = Instant.parse("2026-10-14T10:20:00Z");

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

    @Test
    @DisplayName("accept из PENDING: статус ACCEPTED и время решения, остальные поля и исходная заявка не меняются")
    void whenAcceptPendingThenAccepted() {
        var offer = FriendshipOffer.create(UUID.randomUUID(), UUID.randomUUID(), NOW);

        var accepted = offer.accept(DECIDED_AT);

        assertThat(accepted).isEqualTo(new FriendshipOffer(
                offer.id(), offer.fromUserId(), offer.toUserId(),
                FriendshipOfferStatus.ACCEPTED, NOW, DECIDED_AT
        ));
        assertThat(offer.status()).isEqualTo(FriendshipOfferStatus.PENDING);
    }

    @Test
    @DisplayName("reject из PENDING: статус REJECTED и время решения, остальные поля и исходная заявка не меняются")
    void whenRejectPendingThenRejected() {
        var offer = FriendshipOffer.create(UUID.randomUUID(), UUID.randomUUID(), NOW);

        var rejected = offer.reject(DECIDED_AT);

        assertThat(rejected).isEqualTo(new FriendshipOffer(
                offer.id(), offer.fromUserId(), offer.toUserId(),
                FriendshipOfferStatus.REJECTED, NOW, DECIDED_AT
        ));
        assertThat(offer.status()).isEqualTo(FriendshipOfferStatus.PENDING);
    }

    @ParameterizedTest(name = "{0} заявки в статусе {1} → {2}")
    @MethodSource("decisionsOfDecidedOffer")
    @DisplayName("accept/reject уже принятой или отклонённой заявки: исключение по её статусу")
    void whenDecideDecidedOfferThenThrowExceptionOfItsStatus(
            String command,
            FriendshipOfferStatus status,
            Class<? extends RuntimeException> expected,
            BiFunction<FriendshipOffer, Instant, FriendshipOffer> decision
    ) {
        var offer = new FriendshipOffer(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), status, NOW, NOW
        );

        assertThatThrownBy(() -> decision.apply(offer, DECIDED_AT))
                .isInstanceOf(expected)
                .hasMessageContaining(offer.id().toString());
    }

    private static Stream<Arguments> decisionsOfDecidedOffer() {
        BiFunction<FriendshipOffer, Instant, FriendshipOffer> accept = FriendshipOffer::accept;
        BiFunction<FriendshipOffer, Instant, FriendshipOffer> reject = FriendshipOffer::reject;
        var accepted = FriendshipOfferStatus.ACCEPTED;
        var rejected = FriendshipOfferStatus.REJECTED;
        var alreadyAccepted = FriendshipOfferAlreadyAcceptedException.class;
        var alreadyRejected = FriendshipOfferAlreadyRejectedException.class;
        return Stream.of(
                Arguments.of("accept", accepted, alreadyAccepted, accept),
                Arguments.of("accept", rejected, alreadyRejected, accept),
                Arguments.of("reject", accepted, alreadyAccepted, reject),
                Arguments.of("reject", rejected, alreadyRejected, reject)
        );
    }
}
