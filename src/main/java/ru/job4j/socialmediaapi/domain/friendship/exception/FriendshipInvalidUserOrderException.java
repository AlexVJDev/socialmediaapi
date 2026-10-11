package ru.job4j.socialmediaapi.domain.friendship.exception;

import java.util.UUID;

public class FriendshipInvalidUserOrderException extends RuntimeException {

    public FriendshipInvalidUserOrderException(UUID firstUserId, UUID secondUserId, Throwable cause) {
        super("Friendship users must be ordered: %s must be less than %s".formatted(firstUserId, secondUserId), cause);
    }
}
