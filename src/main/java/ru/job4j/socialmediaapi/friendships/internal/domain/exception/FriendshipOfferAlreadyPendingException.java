package ru.job4j.socialmediaapi.friendships.internal.domain.exception;

import java.util.UUID;

public class FriendshipOfferAlreadyPendingException extends RuntimeException {

    public FriendshipOfferAlreadyPendingException(UUID friendshipOfferId, UUID fromUserId, UUID toUserId) {
        super("Offer friendship %s from user %s to user %s already waits for a decision"
                .formatted(friendshipOfferId, fromUserId, toUserId));
    }
}
