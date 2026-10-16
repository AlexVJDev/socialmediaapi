package ru.job4j.socialmediaapi.friendships.internal.domain.exception;

import java.util.UUID;

public class FriendshipOfferAlreadyRejectedException extends RuntimeException {

    public FriendshipOfferAlreadyRejectedException(UUID friendshipOfferId, UUID fromUserId, UUID toUserId) {
        super("Offer friendship %s from user %s to user %s is already rejected"
                .formatted(friendshipOfferId, fromUserId, toUserId));
    }
}
