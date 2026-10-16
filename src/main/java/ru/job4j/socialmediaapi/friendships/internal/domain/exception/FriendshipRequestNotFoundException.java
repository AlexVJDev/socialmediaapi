package ru.job4j.socialmediaapi.friendships.internal.domain.exception;

import java.util.UUID;

/**
 * The friendship offer is not found: there is no such offer, or the user who decides it
 * is not its recipient (the offer of someone else is not disclosed).
 */
public class FriendshipRequestNotFoundException extends RuntimeException {

    public FriendshipRequestNotFoundException(UUID friendshipOfferId) {
        this(friendshipOfferId, null);
    }

    public FriendshipRequestNotFoundException(UUID friendshipOfferId, Throwable cause) {
        super("Offer friendship %s not found".formatted(friendshipOfferId), cause);
    }
}
