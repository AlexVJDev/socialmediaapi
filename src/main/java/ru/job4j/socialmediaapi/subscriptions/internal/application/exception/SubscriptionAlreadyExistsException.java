package ru.job4j.socialmediaapi.subscriptions.internal.application.exception;

import java.util.UUID;

/**
 * Error of the {@code SubscriptionRepository} port contract: the follower is already
 * subscribed to the user (unique key {@code uq_subscriptions_users}). Thrown by the
 * persistence adapter; not a domain rule, so it lives in {@code application.exception}.
 */
public class SubscriptionAlreadyExistsException extends RuntimeException {

    public SubscriptionAlreadyExistsException(UUID followerId, UUID followedId, Throwable cause) {
        super("Subscription of user %s to user %s already exists".formatted(followerId, followedId), cause);
    }
}
