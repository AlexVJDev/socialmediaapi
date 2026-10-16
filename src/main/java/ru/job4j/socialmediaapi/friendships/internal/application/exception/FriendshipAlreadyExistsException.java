package ru.job4j.socialmediaapi.friendships.internal.application.exception;

import java.util.UUID;

/**
 * Error of the {@code FriendshipsRepository} port contract: the friendship of these users
 * already exists (unique key {@code uq_friendships_users}). Thrown by the persistence
 * adapter; not a domain rule, so it lives in {@code application.exception}.
 */
public class FriendshipAlreadyExistsException extends RuntimeException {

    public FriendshipAlreadyExistsException(UUID firstUserId, UUID secondUserId, Throwable cause) {
        super("Friendship between users %s and %s already exists".formatted(firstUserId, secondUserId), cause);
    }
}
