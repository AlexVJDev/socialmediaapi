package ru.job4j.socialmediaapi.subscriptions.internal.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.subscriptions.internal.application.model.UnsubscribeCommand;
import ru.job4j.socialmediaapi.subscriptions.internal.application.port.SubscriptionRepository;

/**
 * Отписка текущего пользователя от пользователя. Идемпотентна: если подписки нет, ничего не происходит.
 */
@Service
@RequiredArgsConstructor
public class Unsubscribe {

    /** Хранит подписки. */
    private final SubscriptionRepository subscriptionRepository;

    /**
     * @param command подписчик и пользователь, от которого он отписывается
     */
    @Transactional
    public void unsubscribe(UnsubscribeCommand command) {
        subscriptionRepository.deleteSubscription(command.followerId(), command.followedId());
    }
}
