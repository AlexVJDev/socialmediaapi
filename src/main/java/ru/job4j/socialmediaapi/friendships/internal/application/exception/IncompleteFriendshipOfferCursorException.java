package ru.job4j.socialmediaapi.friendships.internal.application.exception;

/**
 * Only one part of the cursor of a page of all offers is set: {@code afterCreatedAt}
 * and {@code afterId} are set together or not at all.
 */
public class IncompleteFriendshipOfferCursorException extends RuntimeException {

    public IncompleteFriendshipOfferCursorException() {
        super("afterCreatedAt and afterId must be set together");
    }
}
