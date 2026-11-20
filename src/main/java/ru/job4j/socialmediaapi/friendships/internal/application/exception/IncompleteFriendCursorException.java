package ru.job4j.socialmediaapi.friendships.internal.application.exception;

/**
 * Задана только одна часть курсора страницы друзей: {@code afterCreatedAt}
 * и {@code afterId} задаются вместе или не задаются вовсе.
 */
public class IncompleteFriendCursorException extends RuntimeException {

    public IncompleteFriendCursorException() {
        super("afterCreatedAt and afterId must be set together");
    }
}
