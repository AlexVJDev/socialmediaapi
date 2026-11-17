package ru.job4j.socialmediaapi.subscriptions.internal.domain.exception;

import java.util.UUID;

/**
 * Пользователь пытается подписаться на самого себя.
 */
public class SubscriptionToSelfException extends RuntimeException {

    /**
     * @param userId пользователь, который подписывается на себя
     */
    public SubscriptionToSelfException(UUID userId) {
        this(userId, null);
    }

    public SubscriptionToSelfException(UUID userId, Throwable cause) {
        super("User %s cannot subscribe to themselves".formatted(userId), cause);
    }
}
