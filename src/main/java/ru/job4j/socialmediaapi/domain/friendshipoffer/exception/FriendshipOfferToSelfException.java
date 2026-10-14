package ru.job4j.socialmediaapi.domain.friendshipoffer.exception;

import java.util.UUID;

public class FriendshipOfferToSelfException extends RuntimeException {

    public FriendshipOfferToSelfException(UUID userId) {
        this(userId, null);
    }

    public FriendshipOfferToSelfException(UUID userId, Throwable cause) {
        super("User %s cannot offer friendship to themselves".formatted(userId), cause);
    }
}
