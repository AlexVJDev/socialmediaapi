package ru.job4j.socialmediaapi.friendships.internal.application.exception;

/**
 * Задана только одна часть курсора страницы заявок: {@code afterCreatedAt}
 * и {@code afterId} задаются вместе или не задаются вовсе.
 */
public class IncompleteFriendshipOfferCursorException extends RuntimeException {

    public IncompleteFriendshipOfferCursorException() {
        super("afterCreatedAt and afterId must be set together");
    }
}
