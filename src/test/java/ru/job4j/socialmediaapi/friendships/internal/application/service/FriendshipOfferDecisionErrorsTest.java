package ru.job4j.socialmediaapi.friendships.internal.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.job4j.socialmediaapi.friendships.internal.application.mapper.FriendshipOfferApplicationMapper;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipOfferRepository;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FindFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyAcceptedException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyRejectedException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipRequestNotFoundException;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Выбор ошибки после неудачного решения по заявке")
class FriendshipOfferDecisionErrorsTest {

    /** Fixed time of the attempt. */
    private static final Instant NOW = Instant.parse("2026-10-16T10:20:00Z");

    /** Offer that could not be decided. */
    private static final UUID OFFER_ID = UUID.randomUUID();

    /** User who tried to decide the offer. */
    private static final UUID ACTOR_ID = UUID.randomUUID();

    /** Mocked offer repository. */
    @Mock
    private FriendshipOfferRepository repository;

    /** Component under test. */
    private FriendshipOfferDecisionErrors errors;

    @BeforeEach
    void setUp() {
        errors = new FriendshipOfferDecisionErrors(
                repository, Mappers.getMapper(FriendshipOfferApplicationMapper.class)
        );
    }

    @Test
    @DisplayName("Заявка не найдена для этого получателя: FriendshipRequestNotFoundException с id заявки")
    void whenNotFoundThenNotFoundError() {
        when(repository.findByIdAndRecipient(OFFER_ID, ACTOR_ID)).thenReturn(Optional.empty());

        assertThat(errors.forAccept(OFFER_ID, ACTOR_ID, NOW))
                .isInstanceOf(FriendshipRequestNotFoundException.class)
                .hasMessageContaining(OFFER_ID.toString());
        assertThat(errors.forReject(OFFER_ID, ACTOR_ID, NOW))
                .isInstanceOf(FriendshipRequestNotFoundException.class);
    }

    @Test
    @DisplayName("Заявка принята: для accept и reject — FriendshipOfferAlreadyAcceptedException")
    void whenAcceptedThenAlreadyAccepted() {
        stored(FriendshipOfferStatus.ACCEPTED);

        assertThat(errors.forAccept(OFFER_ID, ACTOR_ID, NOW))
                .isInstanceOf(FriendshipOfferAlreadyAcceptedException.class);
        assertThat(errors.forReject(OFFER_ID, ACTOR_ID, NOW))
                .isInstanceOf(FriendshipOfferAlreadyAcceptedException.class);
    }

    @Test
    @DisplayName("Заявка отклонена: для accept и reject — FriendshipOfferAlreadyRejectedException")
    void whenRejectedThenAlreadyRejected() {
        stored(FriendshipOfferStatus.REJECTED);

        assertThat(errors.forAccept(OFFER_ID, ACTOR_ID, NOW))
                .isInstanceOf(FriendshipOfferAlreadyRejectedException.class);
        assertThat(errors.forReject(OFFER_ID, ACTOR_ID, NOW))
                .isInstanceOf(FriendshipOfferAlreadyRejectedException.class);
    }

    @Test
    @DisplayName("Заявка всё ещё PENDING (невозможно при корректной БД): IllegalStateException")
    void whenStillPendingThenIllegalState() {
        stored(FriendshipOfferStatus.PENDING);

        assertThat(errors.forAccept(OFFER_ID, ACTOR_ID, NOW)).isInstanceOf(IllegalStateException.class);
    }

    private void stored(FriendshipOfferStatus status) {
        when(repository.findByIdAndRecipient(OFFER_ID, ACTOR_ID)).thenReturn(Optional.of(
                new FindFriendshipOfferResponse(OFFER_ID, UUID.randomUUID(), ACTOR_ID, status, NOW, NOW)
        ));
    }
}
