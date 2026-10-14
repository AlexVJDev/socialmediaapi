package ru.job4j.socialmediaapi.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.job4j.socialmediaapi.application.mapper.FriendshipOfferApplicationMapper;
import ru.job4j.socialmediaapi.application.port.in.CreateFriendshipOfferUseCase.CreateFriendshipOfferCommand;
import ru.job4j.socialmediaapi.application.port.in.CreateFriendshipOfferUseCase.CreateFriendshipOfferResult;
import ru.job4j.socialmediaapi.application.port.out.FriendshipOfferRepository;
import ru.job4j.socialmediaapi.application.port.out.FriendshipOfferRepository.CreateFriendshipOfferRequest;
import ru.job4j.socialmediaapi.application.port.out.FriendshipOfferRepository.CreateFriendshipOfferResponse;
import ru.job4j.socialmediaapi.application.port.out.FriendshipOfferRepository.FindFriendshipOfferResponse;
import ru.job4j.socialmediaapi.application.port.out.SubscriptionRepository;
import ru.job4j.socialmediaapi.application.port.out.SubscriptionRepository.CreateSubscriptionRequest;
import ru.job4j.socialmediaapi.domain.friendshipoffer.FriendshipOfferStatus;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferAlreadyAcceptedException;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferAlreadyExistsException;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferAlreadyRejectedException;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferToSelfException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

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

    /** Mocked subscription repository. */
    @Mock
    private SubscriptionRepository subscriptionRepository;

    /** Service under test. */
    private FriendshipOfferService service;

    @BeforeEach
    void setUp() {
        service = new FriendshipOfferService(
                friendshipOfferRepository,
                subscriptionRepository,
                Mappers.getMapper(FriendshipOfferApplicationMapper.class),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("Заявка самому себе выбросит FriendshipOfferToSelfException без обращения к БД")
    void whenOfferToSelfThenThrowExceptionAndNoRepositoryCalls() {
        var command = new CreateFriendshipOfferCommand(FROM_USER_ID, FROM_USER_ID);

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(FriendshipOfferToSelfException.class);
        verifyNoInteractions(friendshipOfferRepository, subscriptionRepository);
    }

    @Test
    @DisplayName("Заявки нет: создаётся заявка PENDING и подписка инициатора на получателя")
    void whenOfferNotExistsThenCreateOfferAndSubscription() {
        when(friendshipOfferRepository.findByUsers(FROM_USER_ID, TO_USER_ID)).thenReturn(Optional.empty());
        when(friendshipOfferRepository.createFriendshipOffer(any()))
                .thenAnswer(invocation -> saved(invocation.getArgument(0)));

        var result = service.create(command());

        var offerCaptor = ArgumentCaptor.forClass(CreateFriendshipOfferRequest.class);
        verify(friendshipOfferRepository).createFriendshipOffer(offerCaptor.capture());
        var offer = offerCaptor.getValue();
        assertThat(offer.fromUserId()).isEqualTo(FROM_USER_ID);
        assertThat(offer.toUserId()).isEqualTo(TO_USER_ID);
        assertThat(offer.status()).isEqualTo(FriendshipOfferStatus.PENDING);
        assertThat(offer.createdAt()).isEqualTo(NOW);
        assertThat(offer.updatedAt()).isEqualTo(NOW);

        var subscriptionCaptor = ArgumentCaptor.forClass(CreateSubscriptionRequest.class);
        verify(subscriptionRepository).createSubscriptionIfAbsent(subscriptionCaptor.capture());
        var subscription = subscriptionCaptor.getValue();
        assertThat(subscription.followerId()).isEqualTo(FROM_USER_ID);
        assertThat(subscription.followedId()).isEqualTo(TO_USER_ID);
        assertThat(subscription.createdAt()).isEqualTo(NOW);

        assertThat(result).isEqualTo(new CreateFriendshipOfferResult.Created(
                offer.id(), FROM_USER_ID, TO_USER_ID, FriendshipOfferStatus.PENDING, NOW
        ));
    }

    @Test
    @DisplayName("Заявки нет, подписка уже есть (в том числе создана параллельно): заявка всё равно создаётся")
    void whenOfferNotExistsAndSubscriptionExistsThenOfferCreated() {
        when(friendshipOfferRepository.findByUsers(FROM_USER_ID, TO_USER_ID)).thenReturn(Optional.empty());
        when(friendshipOfferRepository.createFriendshipOffer(any()))
                .thenAnswer(invocation -> saved(invocation.getArgument(0)));
        when(subscriptionRepository.createSubscriptionIfAbsent(any())).thenReturn(Optional.empty());

        var result = service.create(command());

        assertThat(result).isInstanceOf(CreateFriendshipOfferResult.Created.class);
        verify(subscriptionRepository, never()).createSubscription(any());
    }

    @Test
    @DisplayName("Повтор при PENDING: AlreadyPending, ничего не создаётся")
    void whenOfferPendingThenAlreadyPending() {
        when(friendshipOfferRepository.findByUsers(FROM_USER_ID, TO_USER_ID))
                .thenReturn(Optional.of(existing(FriendshipOfferStatus.PENDING)));

        var result = service.create(command());

        assertThat(result).isEqualTo(new CreateFriendshipOfferResult.AlreadyPending());
        verifyNothingCreated();
    }

    @Test
    @DisplayName("Повтор при ACCEPTED выбросит FriendshipOfferAlreadyAcceptedException, ничего не создаётся")
    void whenOfferAcceptedThenThrowException() {
        when(friendshipOfferRepository.findByUsers(FROM_USER_ID, TO_USER_ID))
                .thenReturn(Optional.of(existing(FriendshipOfferStatus.ACCEPTED)));

        assertThatThrownBy(() -> service.create(command()))
                .isInstanceOf(FriendshipOfferAlreadyAcceptedException.class);
        verifyNothingCreated();
    }

    @Test
    @DisplayName("Повтор при REJECTED выбросит FriendshipOfferAlreadyRejectedException, ничего не создаётся")
    void whenOfferRejectedThenThrowException() {
        when(friendshipOfferRepository.findByUsers(FROM_USER_ID, TO_USER_ID))
                .thenReturn(Optional.of(existing(FriendshipOfferStatus.REJECTED)));

        assertThatThrownBy(() -> service.create(command()))
                .isInstanceOf(FriendshipOfferAlreadyRejectedException.class);
        verifyNothingCreated();
    }

    @Test
    @DisplayName("Гонка: параллельный запрос успел создать заявку (FriendshipOfferAlreadyExistsException), "
            + "повтор отвечает AlreadyPending без подписки")
    void whenOfferCreatedConcurrentlyThenAlreadyPending() {
        when(friendshipOfferRepository.findByUsers(FROM_USER_ID, TO_USER_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing(FriendshipOfferStatus.PENDING)));
        when(friendshipOfferRepository.createFriendshipOffer(any()))
                .thenThrow(new FriendshipOfferAlreadyExistsException(FROM_USER_ID, TO_USER_ID, null));

        var result = service.create(command());

        assertThat(result).isEqualTo(new CreateFriendshipOfferResult.AlreadyPending());
        verifyNoInteractions(subscriptionRepository);
    }

    private void verifyNothingCreated() {
        verify(friendshipOfferRepository, never()).createFriendshipOffer(any());
        verifyNoInteractions(subscriptionRepository);
    }

    private static CreateFriendshipOfferCommand command() {
        return new CreateFriendshipOfferCommand(FROM_USER_ID, TO_USER_ID);
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
