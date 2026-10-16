package ru.job4j.socialmediaapi.friendships.internal.domain.exception;

import java.util.UUID;

public class FriendshipAlreadyExistsException extends RuntimeException {

    public FriendshipAlreadyExistsException(UUID firstUserId, UUID secondUserId, Throwable cause) {
        super("Friendship between users %s and %s already exists".formatted(firstUserId, secondUserId), cause);
    }
}
