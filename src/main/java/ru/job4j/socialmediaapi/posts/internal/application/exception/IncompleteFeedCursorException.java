package ru.job4j.socialmediaapi.posts.internal.application.exception;

/**
 * Only one part of the cursor of a feed page is set: {@code afterCreatedAt}
 * and {@code afterId} are set together or not at all.
 */
public class IncompleteFeedCursorException extends RuntimeException {

    public IncompleteFeedCursorException() {
        super("afterCreatedAt and afterId must be set together");
    }
}
