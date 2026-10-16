package ru.job4j.socialmediaapi.friendships.internal.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.job4j.socialmediaapi.friendships.internal.application.mapper.FriendshipOfferApplicationMapper;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferDecisionResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.RejectFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipOfferRepository;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FindFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.UpdateFriendshipOfferStatusRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.UpdateFriendshipOfferStatusResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.service.FriendshipOfferDecisionErrors;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyAcceptedException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyRejectedException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipRequestNotFoundException;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Сценарий отклонения заявки в друзья: ветка «3. Отклонение» friendship_transitions.puml")
class RejectFriendshipOfferTest {

    /** Fixed time returned by the test clock. */
    private static final Instant NOW = Instant.parse("2026-10-16T10:20:00Z");

    /** Creation time of the offer. */
    private static final Instant CREATED_AT = Instant.parse("2026-10-16T10:15:00Z");

    /** Sender of the offer. */
    private static final UUID FROM_USER_ID = UUID.randomUUID();

    /** Recipient of the offer, who rejects it. */
    private static final UUID TO_USER_ID = UUID.randomUUID();

    /** Offer to reject. */
    private static final UUID OFFER_ID = UUID.randomUUID();

    /** Mocked offer repository; the scenario has no other ports, so it cannot touch friendships or subscriptions. */
    @Mock
    private FriendshipOfferRepository friendshipOfferRepository;

    /** Use case under test. */
    private RejectFriendshipOffer useCase;

    @BeforeEach
    void setUp() {
        var mapper = Mappers.getMapper(FriendshipOfferApplicationMapper.class);
        useCase = new RejectFriendshipOffer(
                friendshipOfferRepository,
                new FriendshipOfferDecisionErrors(friendshipOfferRepository, mapper),
                mapper,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("PENDING: статус REJECTED, результат — заявка с временем решения")
    void whenPendingThenRejected() {
        when(friendshipOfferRepository.updateStatusIfPending(any())).thenReturn(Optional.of(
                new UpdateFriendshipOfferStatusResponse(
                        OFFER_ID, FROM_USER_ID, TO_USER_ID, FriendshipOfferStatus.REJECTED, CREATED_AT, NOW
                )
        ));

        var result = useCase.reject(command());

        verify(friendshipOfferRepository).updateStatusIfPending(new UpdateFriendshipOfferStatusRequest(
                OFFER_ID, TO_USER_ID, FriendshipOfferStatus.REJECTED, NOW
        ));
        assertThat(result).isEqualTo(new FriendshipOfferDecisionResult(
                OFFER_ID, FROM_USER_ID, TO_USER_ID, FriendshipOfferStatus.REJECTED, CREATED_AT, NOW
        ));
    }

    @Test
    @DisplayName("Заявки нет или отклоняет не получатель: FriendshipRequestNotFoundException")
    void whenOfferNotFoundThenThrowNotFound() {
        when(friendshipOfferRepository.updateStatusIfPending(any())).thenReturn(Optional.empty());
        when(friendshipOfferRepository.findByIdAndRecipient(OFFER_ID, TO_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.reject(command()))
                .isInstanceOf(FriendshipRequestNotFoundException.class);
    }

    @ParameterizedTest
    @MethodSource("decidedStatusErrors")
    @DisplayName("Заявка уже решена: исключение по её статусу")
    void whenOfferDecidedThenThrowExceptionOfItsStatus(
            FriendshipOfferStatus status, Class<? extends RuntimeException> expected
    ) {
        when(friendshipOfferRepository.updateStatusIfPending(any())).thenReturn(Optional.empty());
        when(friendshipOfferRepository.findByIdAndRecipient(OFFER_ID, TO_USER_ID)).thenReturn(Optional.of(
                new FindFriendshipOfferResponse(OFFER_ID, FROM_USER_ID, TO_USER_ID, status, CREATED_AT, CREATED_AT)
        ));

        assertThatThrownBy(() -> useCase.reject(command())).isInstanceOf(expected);
    }

    private static Stream<Arguments> decidedStatusErrors() {
        return Stream.of(
                Arguments.of(FriendshipOfferStatus.ACCEPTED, FriendshipOfferAlreadyAcceptedException.class),
                Arguments.of(FriendshipOfferStatus.REJECTED, FriendshipOfferAlreadyRejectedException.class)
        );
    }

    private static RejectFriendshipOfferCommand command() {
        return new RejectFriendshipOfferCommand(OFFER_ID, TO_USER_ID);
    }
}
