package ru.job4j.socialmediaapi.domain.subscription.exception;

public class SubscriptionUserRequiredException extends RuntimeException {

    public SubscriptionUserRequiredException(String column, Throwable cause) {
        super("Subscription %s is required".formatted(column), cause);
    }
}
