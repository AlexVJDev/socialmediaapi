package ru.job4j.socialmediaapi.domain.offerfriendship.exception;

import java.util.UUID;

public class OfferFriendshipAlreadyRejectedException extends RuntimeException {

    public OfferFriendshipAlreadyRejectedException(UUID offerFriendshipId, UUID fromUserId, UUID toUserId) {
        super("Offer friendship %s from user %s to user %s is already rejected"
                .formatted(offerFriendshipId, fromUserId, toUserId));
    }
}
