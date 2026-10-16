package ru.job4j.socialmediaapi.friendships.internal.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.job4j.socialmediaapi.friendships.internal.application.mapper.FriendshipApplicationMapper;
import ru.job4j.socialmediaapi.friendships.internal.application.mapper.FriendshipOfferApplicationMapper;
import ru.job4j.socialmediaapi.friendships.internal.application.model.AcceptFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferDecisionResult;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipOfferRepository;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipsRepository;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FindFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.UpdateFriendshipOfferStatusRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.UpdateFriendshipOfferStatusResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.service.FriendshipOfferDecisionErrors;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyAcceptedException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyRejectedException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipRequestNotFoundException;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipStatus;
import ru.job4j.socialmediaapi.subscriptions.api.Subscriptions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Сценарий принятия заявки в друзья: ветка «2. Принятие» friendship_transitions.puml")
class AcceptFriendshipOfferTest {

    /** Fixed time returned by the test clock. */
    private static final Instant NOW = Instant.parse("2026-10-16T10:20:00Z");

    /** Creation time of the offer. */
    private static final Instant CREATED_AT = Instant.parse("2026-10-16T10:15:00Z");

    /** Sender of the offer; greater than {@link #TO_USER_ID} in the PostgreSQL order. */
    private static final UUID FROM_USER_ID = UUID.fromString("80000000-0000-0000-0000-000000000000");

    /** Recipient of the offer, who accepts it. */
    private static final UUID TO_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    /** Offer to accept. */
    private static final UUID OFFER_ID = UUID.randomUUID();

    /** Mocked offer repository. */
    @Mock
    private FriendshipOfferRepository friendshipOfferRepository;

    /** Mocked friendship repository. */
    @Mock
    private FriendshipsRepository friendshipsRepository;

    /** Mocked API of the subscriptions module. */
    @Mock
    private Subscriptions subscriptions;

    /** Use case under test. */
    private AcceptFriendshipOffer useCase;

    @BeforeEach
    void setUp() {
        var offerMapper = Mappers.getMapper(FriendshipOfferApplicationMapper.class);
        useCase = new AcceptFriendshipOffer(
                friendshipOfferRepository,
                friendshipsRepository,
                subscriptions,
                new FriendshipOfferDecisionErrors(friendshipOfferRepository, offerMapper),
                offerMapper,
                Mappers.getMapper(FriendshipApplicationMapper.class),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("PENDING: статус ACCEPTED, дружба в каноническом порядке и подписка получателя на отправителя")
    void whenPendingThenAcceptedFriendshipAndBackSubscription() {
        when(friendshipOfferRepository.updateStatusIfPending(any())).thenReturn(Optional.of(
                new UpdateFriendshipOfferStatusResponse(
                        OFFER_ID, FROM_USER_ID, TO_USER_ID, FriendshipOfferStatus.ACCEPTED, CREATED_AT, NOW
                )
        ));

        var result = useCase.accept(command());

        verify(friendshipOfferRepository).updateStatusIfPending(new UpdateFriendshipOfferStatusRequest(
                OFFER_ID, TO_USER_ID, FriendshipOfferStatus.ACCEPTED, NOW
        ));
        var friendship = ArgumentCaptor.forClass(CreateFriendshipRequest.class);
        verify(friendshipsRepository).createFriendship(friendship.capture());
        assertThat(friendship.getValue().id()).isNotNull();
        assertThat(friendship.getValue().firstUserId()).isEqualTo(TO_USER_ID);
        assertThat(friendship.getValue().secondUserId()).isEqualTo(FROM_USER_ID);
        assertThat(friendship.getValue().friendshipOfferId()).isEqualTo(OFFER_ID);
        assertThat(friendship.getValue().status()).isEqualTo(FriendshipStatus.ACTIVE);
        assertThat(friendship.getValue().createdAt()).isEqualTo(NOW);
        verify(subscriptions).subscribeIfAbsent(TO_USER_ID, FROM_USER_ID, NOW);
        assertThat(result).isEqualTo(new FriendshipOfferDecisionResult(
                OFFER_ID, FROM_USER_ID, TO_USER_ID, FriendshipOfferStatus.ACCEPTED, CREATED_AT, NOW
        ));
    }

    @Test
    @DisplayName("Заявки нет или принимает не получатель: FriendshipRequestNotFoundException, дружбы и подписки нет")
    void whenOfferNotFoundThenThrowNotFound() {
        when(friendshipOfferRepository.updateStatusIfPending(any())).thenReturn(Optional.empty());
        when(friendshipOfferRepository.findByIdAndRecipient(OFFER_ID, TO_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.accept(command()))
                .isInstanceOf(FriendshipRequestNotFoundException.class);
        verifyNoInteractions(friendshipsRepository, subscriptions);
    }

    @ParameterizedTest
    @MethodSource("decidedStatusErrors")
    @DisplayName("Заявка уже решена: исключение по её статусу, дружбы и подписки нет")
    void whenOfferDecidedThenThrowExceptionOfItsStatus(
            FriendshipOfferStatus status, Class<? extends RuntimeException> expected
    ) {
        when(friendshipOfferRepository.updateStatusIfPending(any())).thenReturn(Optional.empty());
        when(friendshipOfferRepository.findByIdAndRecipient(OFFER_ID, TO_USER_ID)).thenReturn(Optional.of(
                new FindFriendshipOfferResponse(OFFER_ID, FROM_USER_ID, TO_USER_ID, status, CREATED_AT, CREATED_AT)
        ));

        assertThatThrownBy(() -> useCase.accept(command())).isInstanceOf(expected);
        verifyNoInteractions(friendshipsRepository, subscriptions);
    }

    private static Stream<Arguments> decidedStatusErrors() {
        return Stream.of(
                Arguments.of(FriendshipOfferStatus.ACCEPTED, FriendshipOfferAlreadyAcceptedException.class),
                Arguments.of(FriendshipOfferStatus.REJECTED, FriendshipOfferAlreadyRejectedException.class)
        );
    }

    private static AcceptFriendshipOfferCommand command() {
        return new AcceptFriendshipOfferCommand(OFFER_ID, TO_USER_ID);
    }
}
