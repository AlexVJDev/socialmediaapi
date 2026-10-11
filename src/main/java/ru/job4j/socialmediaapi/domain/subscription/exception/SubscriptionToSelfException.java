package ru.job4j.socialmediaapi.domain.subscription.exception;

import java.util.UUID;

public class SubscriptionToSelfException extends RuntimeException {

    public SubscriptionToSelfException(UUID userId, Throwable cause) {
        super("User %s cannot subscribe to themselves".formatted(userId), cause);
    }
}
