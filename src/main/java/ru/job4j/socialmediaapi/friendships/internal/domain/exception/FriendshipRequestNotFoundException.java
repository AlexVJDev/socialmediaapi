package ru.job4j.socialmediaapi.friendships.internal.domain.exception;

import java.util.UUID;

public class FriendshipRequestNotFoundException extends RuntimeException {

    public FriendshipRequestNotFoundException(UUID friendshipOfferId, Throwable cause) {
        super("Offer friendship %s not found".formatted(friendshipOfferId), cause);
    }
}
