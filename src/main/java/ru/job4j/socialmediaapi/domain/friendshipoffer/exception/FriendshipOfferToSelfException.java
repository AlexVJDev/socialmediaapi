package ru.job4j.socialmediaapi.domain.friendshipoffer.exception;

import java.util.UUID;

public class FriendshipOfferToSelfException extends RuntimeException {

    public FriendshipOfferToSelfException(UUID userId) {
        super("User %s cannot offer friendship to themselves".formatted(userId));
    }
}
