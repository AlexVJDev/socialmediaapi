package ru.job4j.socialmediaapi.subscriptions.internal.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.job4j.socialmediaapi.subscriptions.internal.application.mapper.SubscriptionApplicationMapper;
import ru.job4j.socialmediaapi.subscriptions.internal.application.model.SubscribeCommand;
import ru.job4j.socialmediaapi.subscriptions.internal.application.model.SubscribeResult;
import ru.job4j.socialmediaapi.subscriptions.internal.application.model.SubscriptionResult;
import ru.job4j.socialmediaapi.subscriptions.internal.application.port.SubscriptionRepository;
import ru.job4j.socialmediaapi.subscriptions.internal.application.port.model.SubscriptionResponse;
import ru.job4j.socialmediaapi.subscriptions.internal.domain.exception.SubscriptionToSelfException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Сценарий подписки")
class SubscribeTest {

    /** Текущее время сценария. */
    private static final Instant NOW = Instant.parse("2026-10-15T10:00:00Z");

    /** Подписчик. */
    private static final UUID FOLLOWER = UUID.randomUUID();

    /** Пользователь, на которого подписываются. */
    private static final UUID FOLLOWED = UUID.randomUUID();

    /** Мок хранилища подписок. */
    @Mock
    private SubscriptionRepository subscriptionRepository;

    /** Проверяемый сценарий. */
    private Subscribe useCase;

    @BeforeEach
    void setUp() {
        useCase = new Subscribe(subscriptionRepository, Mappers.getMapper(SubscriptionApplicationMapper.class),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Новая подписка: created = true")
    void whenNewThenCreated() {
        var id = UUID.randomUUID();
        when(subscriptionRepository.createSubscriptionIfAbsent(any()))
                .thenReturn(Optional.of(new SubscriptionResponse(id, FOLLOWER, FOLLOWED, NOW)));

        assertThat(useCase.subscribe(new SubscribeCommand(FOLLOWER, FOLLOWED)))
                .isEqualTo(new SubscribeResult(new SubscriptionResult(id, FOLLOWER, FOLLOWED, NOW), true));
    }

    @Test
    @DisplayName("Повторная подписка: существующая подписка и created = false")
    void whenExistsThenExisting() {
        var id = UUID.randomUUID();
        var earlier = NOW.minusSeconds(3600);
        when(subscriptionRepository.createSubscriptionIfAbsent(any())).thenReturn(Optional.empty());
        when(subscriptionRepository.findSubscription(FOLLOWER, FOLLOWED))
                .thenReturn(Optional.of(new SubscriptionResponse(id, FOLLOWER, FOLLOWED, earlier)));

        assertThat(useCase.subscribe(new SubscribeCommand(FOLLOWER, FOLLOWED)))
                .isEqualTo(new SubscribeResult(new SubscriptionResult(id, FOLLOWER, FOLLOWED, earlier), false));
    }

    @Test
    @DisplayName("Подписка на себя: SubscriptionToSelfException")
    void whenSelfThenException() {
        assertThatThrownBy(() -> useCase.subscribe(new SubscribeCommand(FOLLOWER, FOLLOWER)))
                .isInstanceOf(SubscriptionToSelfException.class);
    }
}
