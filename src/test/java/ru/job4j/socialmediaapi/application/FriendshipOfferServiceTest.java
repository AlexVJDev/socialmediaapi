package ru.job4j.socialmediaapi.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.job4j.socialmediaapi.application.mapper.OfferFriendshipApplicationMapper;
import ru.job4j.socialmediaapi.application.port.in.CreateOfferFriendshipUseCase.CreateOfferFriendshipCommand;
import ru.job4j.socialmediaapi.application.port.in.CreateOfferFriendshipUseCase.CreateOfferFriendshipResult;
import ru.job4j.socialmediaapi.application.port.out.OfferFriendshipRepository;
import ru.job4j.socialmediaapi.application.port.out.OfferFriendshipRepository.CreateOfferFriendshipRequest;
import ru.job4j.socialmediaapi.application.port.out.OfferFriendshipRepository.CreateOfferFriendshipResponse;
import ru.job4j.socialmediaapi.application.port.out.OfferFriendshipRepository.FindOfferFriendshipResponse;
import ru.job4j.socialmediaapi.application.port.out.SubscriptionRepository;
import ru.job4j.socialmediaapi.application.port.out.SubscriptionRepository.CreateSubscriptionRequest;
import ru.job4j.socialmediaapi.domain.offerfriendship.OfferFriendshipStatus;
import ru.job4j.socialmediaapi.domain.offerfriendship.exception.OfferFriendshipAlreadyAcceptedException;
import ru.job4j.socialmediaapi.domain.offerfriendship.exception.OfferFriendshipAlreadyRejectedException;
import ru.job4j.socialmediaapi.domain.offerfriendship.exception.OfferFriendshipToSelfException;

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
class OfferFriendshipServiceTest {

    /** Fixed time returned by the test clock. */
    private static final Instant NOW = Instant.parse("2026-10-12T10:15:00Z");

    /** Sender of the offer. */
    private static final UUID FROM_USER_ID = UUID.randomUUID();

    /** Recipient of the offer. */
    private static final UUID TO_USER_ID = UUID.randomUUID();

    /** Mocked offer repository. */
    @Mock
    private OfferFriendshipRepository offerFriendshipRepository;

    /** Mocked subscription repository. */
    @Mock
    private SubscriptionRepository subscriptionRepository;

    /** Service under test. */
    private OfferFriendshipService service;

    @BeforeEach
    void setUp() {
        service = new OfferFriendshipService(
                offerFriendshipRepository,
                subscriptionRepository,
                Mappers.getMapper(OfferFriendshipApplicationMapper.class),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("Заявка самому себе выбросит OfferFriendshipToSelfException без обращения к БД")
    void whenOfferToSelfThenThrowExceptionAndNoRepositoryCalls() {
        var command = new CreateOfferFriendshipCommand(FROM_USER_ID, FROM_USER_ID);

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(OfferFriendshipToSelfException.class);
        verifyNoInteractions(offerFriendshipRepository, subscriptionRepository);
    }

    @Test
    @DisplayName("Заявки нет: создаётся заявка PENDING и подписка инициатора на получателя")
    void whenOfferNotExistsThenCreateOfferAndSubscription() {
        when(offerFriendshipRepository.findByUsers(FROM_USER_ID, TO_USER_ID)).thenReturn(Optional.empty());
        when(offerFriendshipRepository.createOfferFriendshipIfAbsent(any()))
                .thenAnswer(invocation -> Optional.of(saved(invocation.getArgument(0))));
        when(subscriptionRepository.existsSubscription(FROM_USER_ID, TO_USER_ID)).thenReturn(false);

        var result = service.create(command());

        var offerCaptor = ArgumentCaptor.forClass(CreateOfferFriendshipRequest.class);
        verify(offerFriendshipRepository).createOfferFriendshipIfAbsent(offerCaptor.capture());
        var offer = offerCaptor.getValue();
        assertThat(offer.fromUserId()).isEqualTo(FROM_USER_ID);
        assertThat(offer.toUserId()).isEqualTo(TO_USER_ID);
        assertThat(offer.status()).isEqualTo(OfferFriendshipStatus.PENDING);
        assertThat(offer.createdAt()).isEqualTo(NOW);
        assertThat(offer.updatedAt()).isEqualTo(NOW);

        var subscriptionCaptor = ArgumentCaptor.forClass(CreateSubscriptionRequest.class);
        verify(subscriptionRepository).createSubscription(subscriptionCaptor.capture());
        var subscription = subscriptionCaptor.getValue();
        assertThat(subscription.followerId()).isEqualTo(FROM_USER_ID);
        assertThat(subscription.followedId()).isEqualTo(TO_USER_ID);
        assertThat(subscription.createdAt()).isEqualTo(NOW);

        assertThat(result).isEqualTo(new CreateOfferFriendshipResult.Created(
                offer.id(), FROM_USER_ID, TO_USER_ID, OfferFriendshipStatus.PENDING, NOW
        ));
    }

    @Test
    @DisplayName("Заявки нет, подписка уже есть: создаётся только заявка")
    void whenOfferNotExistsAndSubscriptionExistsThenCreateOnlyOffer() {
        when(offerFriendshipRepository.findByUsers(FROM_USER_ID, TO_USER_ID)).thenReturn(Optional.empty());
        when(offerFriendshipRepository.createOfferFriendshipIfAbsent(any()))
                .thenAnswer(invocation -> Optional.of(saved(invocation.getArgument(0))));
        when(subscriptionRepository.existsSubscription(FROM_USER_ID, TO_USER_ID)).thenReturn(true);

        var result = service.create(command());

        assertThat(result).isInstanceOf(CreateOfferFriendshipResult.Created.class);
        verify(subscriptionRepository, never()).createSubscription(any());
    }

    @Test
    @DisplayName("Повтор при PENDING: AlreadyPending, ничего не создаётся")
    void whenOfferPendingThenAlreadyPending() {
        when(offerFriendshipRepository.findByUsers(FROM_USER_ID, TO_USER_ID))
                .thenReturn(Optional.of(existing(OfferFriendshipStatus.PENDING)));

        var result = service.create(command());

        assertThat(result).isEqualTo(new CreateOfferFriendshipResult.AlreadyPending());
        verifyNothingCreated();
    }

    @Test
    @DisplayName("Повтор при ACCEPTED выбросит OfferFriendshipAlreadyAcceptedException, ничего не создаётся")
    void whenOfferAcceptedThenThrowException() {
        when(offerFriendshipRepository.findByUsers(FROM_USER_ID, TO_USER_ID))
                .thenReturn(Optional.of(existing(OfferFriendshipStatus.ACCEPTED)));

        assertThatThrownBy(() -> service.create(command()))
                .isInstanceOf(OfferFriendshipAlreadyAcceptedException.class);
        verifyNothingCreated();
    }

    @Test
    @DisplayName("Повтор при REJECTED выбросит OfferFriendshipAlreadyRejectedException, ничего не создаётся")
    void whenOfferRejectedThenThrowException() {
        when(offerFriendshipRepository.findByUsers(FROM_USER_ID, TO_USER_ID))
                .thenReturn(Optional.of(existing(OfferFriendshipStatus.REJECTED)));

        assertThatThrownBy(() -> service.create(command()))
                .isInstanceOf(OfferFriendshipAlreadyRejectedException.class);
        verifyNothingCreated();
    }

    @Test
    @DisplayName("Гонка: параллельный запрос успел создать заявку, повтор отвечает AlreadyPending без подписки")
    void whenOfferCreatedConcurrentlyThenAlreadyPending() {
        when(offerFriendshipRepository.findByUsers(FROM_USER_ID, TO_USER_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing(OfferFriendshipStatus.PENDING)));
        when(offerFriendshipRepository.createOfferFriendshipIfAbsent(any())).thenReturn(Optional.empty());

        var result = service.create(command());

        assertThat(result).isEqualTo(new CreateOfferFriendshipResult.AlreadyPending());
        verifyNoInteractions(subscriptionRepository);
    }

    private void verifyNothingCreated() {
        verify(offerFriendshipRepository, never()).createOfferFriendshipIfAbsent(any());
        verifyNoInteractions(subscriptionRepository);
    }

    private static CreateOfferFriendshipCommand command() {
        return new CreateOfferFriendshipCommand(FROM_USER_ID, TO_USER_ID);
    }

    private static FindOfferFriendshipResponse existing(OfferFriendshipStatus status) {
        return new FindOfferFriendshipResponse(
                UUID.randomUUID(), FROM_USER_ID, TO_USER_ID, status, NOW, NOW
        );
    }

    private static CreateOfferFriendshipResponse saved(CreateOfferFriendshipRequest request) {
        return new CreateOfferFriendshipResponse(
                request.id(), request.fromUserId(), request.toUserId(),
                request.status(), request.createdAt(), request.updatedAt()
        );
    }
}
