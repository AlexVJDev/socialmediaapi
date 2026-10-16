package ru.job4j.socialmediaapi.subscriptions.internal.domain.exception;

import java.util.UUID;

public class SubscriptionAlreadyExistsException extends RuntimeException {

    public SubscriptionAlreadyExistsException(UUID followerId, UUID followedId, Throwable cause) {
        super("Subscription of user %s to user %s already exists".formatted(followerId, followedId), cause);
    }
}
