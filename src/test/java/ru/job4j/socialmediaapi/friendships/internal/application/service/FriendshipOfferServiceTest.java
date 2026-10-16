package ru.job4j.socialmediaapi.friendships.internal.application.service;

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
import ru.job4j.socialmediaapi.friendships.internal.application.mapper.FriendshipOfferApplicationMapper;
import ru.job4j.socialmediaapi.friendships.internal.application.service.CreateFriendshipOfferUseCase.CreateFriendshipOfferUseCaseRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.service.CreateFriendshipOfferUseCase.CreateFriendshipOfferResult;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipOfferRepository;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipOfferRepository.CreateFriendshipOfferRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipOfferRepository.CreateFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipOfferRepository.FindFriendshipOfferResponse;
import ru.job4j.socialmediaapi.subscriptions.api.Subscriptions;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyAcceptedException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyExistsException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyPendingException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyRejectedException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferToSelfException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Сервис заявок в друзья: создание заявки по схеме friendship_transitions.puml")
class FriendshipOfferServiceTest {

    /** Fixed time returned by the test clock. */
    private static final Instant NOW = Instant.parse("2026-10-12T10:15:00Z");

    /** Sender of the offer. */
    private static final UUID FROM_USER_ID = UUID.randomUUID();

    /** Recipient of the offer. */
    private static final UUID TO_USER_ID = UUID.randomUUID();

    /** Mocked offer repository. */
    @Mock
    private FriendshipOfferRepository friendshipOfferRepository;

    /** Mocked API of the subscriptions module. */
    @Mock
    private Subscriptions subscriptions;

    /** Service under test. */
    private FriendshipOfferService service;

    @BeforeEach
    void setUp() {
        service = new FriendshipOfferService(
                friendshipOfferRepository,
                subscriptions,
                Mappers.getMapper(FriendshipOfferApplicationMapper.class),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("Заявка самому себе выбросит FriendshipOfferToSelfException без обращения к БД")
    void whenOfferToSelfThenThrowExceptionAndNoRepositoryCalls() {
        var useCaseRequest = new CreateFriendshipOfferUseCaseRequest(FROM_USER_ID, FROM_USER_ID);

        assertThatThrownBy(() -> service.create(useCaseRequest))
                .isInstanceOf(FriendshipOfferToSelfException.class);
        verifyNoInteractions(friendshipOfferRepository, subscriptions);
    }

    @Test
    @DisplayName("Заявки нет: создаётся заявка PENDING и подписка инициатора на получателя")
    void whenOfferNotExistsThenCreateOfferAndSubscription() {
        when(friendshipOfferRepository.createFriendshipOffer(any()))
                .thenAnswer(invocation -> saved(invocation.getArgument(0)));

        var result = service.create(useCaseRequest());

        var offerCaptor = ArgumentCaptor.forClass(CreateFriendshipOfferRequest.class);
        verify(friendshipOfferRepository).createFriendshipOffer(offerCaptor.capture());
        var offer = offerCaptor.getValue();
        assertThat(offer.fromUserId()).isEqualTo(FROM_USER_ID);
        assertThat(offer.toUserId()).isEqualTo(TO_USER_ID);
        assertThat(offer.status()).isEqualTo(FriendshipOfferStatus.PENDING);
        assertThat(offer.createdAt()).isEqualTo(NOW);
        assertThat(offer.updatedAt()).isEqualTo(NOW);

        verify(subscriptions).subscribeIfAbsent(FROM_USER_ID, TO_USER_ID, NOW);

        assertThat(result).isEqualTo(new CreateFriendshipOfferResult(
                offer.id(), FROM_USER_ID, TO_USER_ID, FriendshipOfferStatus.PENDING, NOW
        ));
    }

    @Test
    @DisplayName("Заявки нет, подписка уже есть (в том числе создана параллельно): заявка всё равно создаётся")
    void whenOfferNotExistsAndSubscriptionExistsThenOfferCreated() {
        when(friendshipOfferRepository.createFriendshipOffer(any()))
                .thenAnswer(invocation -> saved(invocation.getArgument(0)));
        when(subscriptions.subscribeIfAbsent(FROM_USER_ID, TO_USER_ID, NOW)).thenReturn(false);

        var result = service.create(useCaseRequest());

        assertThat(result.fromUserId()).isEqualTo(FROM_USER_ID);
        verify(friendshipOfferRepository, never()).findByUsers(any(), any());
    }

    @ParameterizedTest
    @MethodSource("existingStatusErrors")
    @DisplayName("Заявка уже есть (в том числе создана параллельно): исключение по её статусу, подписка не создаётся")
    void whenOfferAlreadyExistsThenThrowExceptionOfItsStatus(
            FriendshipOfferStatus status, Class<? extends RuntimeException> expected
    ) {
        when(friendshipOfferRepository.createFriendshipOffer(any()))
                .thenThrow(new FriendshipOfferAlreadyExistsException(FROM_USER_ID, TO_USER_ID, null));
        when(friendshipOfferRepository.findByUsers(FROM_USER_ID, TO_USER_ID))
                .thenReturn(Optional.of(existing(status)));

        assertThatThrownBy(() -> service.create(useCaseRequest())).isInstanceOf(expected);
        verifyNoInteractions(subscriptions);
    }

    @Test
    @DisplayName("Дубликат по уникальному ключу, но заявка не найдена: пробрасывается исходное исключение")
    void whenDuplicateButOfferNotFoundThenThrowOriginalException() {
        var duplicate = new FriendshipOfferAlreadyExistsException(FROM_USER_ID, TO_USER_ID, null);
        when(friendshipOfferRepository.createFriendshipOffer(any())).thenThrow(duplicate);
        when(friendshipOfferRepository.findByUsers(FROM_USER_ID, TO_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(useCaseRequest())).isSameAs(duplicate);
        verifyNoInteractions(subscriptions);
    }

    private static Stream<Arguments> existingStatusErrors() {
        return Stream.of(
                Arguments.of(FriendshipOfferStatus.PENDING, FriendshipOfferAlreadyPendingException.class),
                Arguments.of(FriendshipOfferStatus.ACCEPTED, FriendshipOfferAlreadyAcceptedException.class),
                Arguments.of(FriendshipOfferStatus.REJECTED, FriendshipOfferAlreadyRejectedException.class)
        );
    }

    private static CreateFriendshipOfferUseCaseRequest useCaseRequest() {
        return new CreateFriendshipOfferUseCaseRequest(FROM_USER_ID, TO_USER_ID);
    }

    private static FindFriendshipOfferResponse existing(FriendshipOfferStatus status) {
        return new FindFriendshipOfferResponse(
                UUID.randomUUID(), FROM_USER_ID, TO_USER_ID, status, NOW, NOW
        );
    }

    private static CreateFriendshipOfferResponse saved(CreateFriendshipOfferRequest request) {
        return new CreateFriendshipOfferResponse(
                request.id(), request.fromUserId(), request.toUserId(),
                request.status(), request.createdAt(), request.updatedAt()
        );
    }
}
