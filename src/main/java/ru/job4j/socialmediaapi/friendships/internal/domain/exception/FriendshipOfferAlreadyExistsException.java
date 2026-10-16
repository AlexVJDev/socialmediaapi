package ru.job4j.socialmediaapi.friendships.internal.domain.exception;

import java.util.UUID;

public class FriendshipOfferAlreadyExistsException extends RuntimeException {

    public FriendshipOfferAlreadyExistsException(UUID fromUserId, UUID toUserId, Throwable cause) {
        super("Offer friendship from user %s to user %s already exists".formatted(fromUserId, toUserId), cause);
    }
}
