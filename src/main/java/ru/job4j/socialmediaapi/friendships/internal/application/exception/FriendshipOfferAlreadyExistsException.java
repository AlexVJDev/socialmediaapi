package ru.job4j.socialmediaapi.friendships.internal.application.exception;

import java.util.UUID;

/**
 * Error of the {@code FriendshipOfferRepository} port contract: an offer in the same
 * direction already exists (unique key {@code uq_friendship_offer_users}). Thrown by the
 * persistence adapter; not a domain rule, so it lives in {@code application.exception}.
 */
public class FriendshipOfferAlreadyExistsException extends RuntimeException {

    public FriendshipOfferAlreadyExistsException(UUID fromUserId, UUID toUserId, Throwable cause) {
        super("Offer friendship from user %s to user %s already exists".formatted(fromUserId, toUserId), cause);
    }
}
