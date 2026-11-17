package ru.job4j.socialmediaapi.subscriptions.internal.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.subscriptions.internal.application.mapper.SubscriptionApplicationMapper;
import ru.job4j.socialmediaapi.subscriptions.internal.application.model.SubscribeCommand;
import ru.job4j.socialmediaapi.subscriptions.internal.application.model.SubscribeResult;
import ru.job4j.socialmediaapi.subscriptions.internal.application.port.SubscriptionRepository;
import ru.job4j.socialmediaapi.subscriptions.internal.application.port.model.CreateSubscriptionRequest;
import ru.job4j.socialmediaapi.subscriptions.internal.domain.exception.SubscriptionToSelfException;

import java.time.Clock;
import java.util.UUID;

/**
 * Подписка текущего пользователя на пользователя. Повторная подписка не ошибка:
 * возвращается существующая. Вставка атомарна ({@code ON CONFLICT DO NOTHING}), без проверки перед ней.
 */
@Service
@RequiredArgsConstructor
public class Subscribe {

    /** Хранит подписки. */
    private final SubscriptionRepository subscriptionRepository;

    /** Переводит ответ порта в результат. */
    private final SubscriptionApplicationMapper mapper;

    /** Источник текущего времени. */
    private final Clock clock;

    /**
     * @param command подписчик и пользователь, на которого он подписывается
     * @return подписка и признак, создана ли она сейчас
     * @throws SubscriptionToSelfException пользователь подписывается на самого себя
     */
    @Transactional
    public SubscribeResult subscribe(SubscribeCommand command) {
        if (command.followerId().equals(command.followedId())) {
            throw new SubscriptionToSelfException(command.followerId());
        }
        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                UUID.randomUUID(), command.followerId(), command.followedId(), clock.instant());
        return subscriptionRepository.createSubscriptionIfAbsent(request)
                .map(created -> new SubscribeResult(mapper.toResult(created), true))
                .orElseGet(() -> new SubscribeResult(mapper.toResult(
                        subscriptionRepository.findSubscription(command.followerId(), command.followedId())
                                .orElseThrow()), false));
    }
}
