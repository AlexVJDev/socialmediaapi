package ru.job4j.socialmediaapi.friendships.internal.domain.exception;

import java.util.UUID;

public class FriendshipOfferAlreadyAcceptedException extends RuntimeException {

    public FriendshipOfferAlreadyAcceptedException(UUID friendshipOfferId, UUID fromUserId, UUID toUserId) {
        super("Offer friendship %s from user %s to user %s is already accepted"
                .formatted(friendshipOfferId, fromUserId, toUserId));
    }
}
