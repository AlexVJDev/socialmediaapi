package ru.job4j.socialmediaapi.domain.offerfriendship.exception;

import java.util.UUID;

public class OfferFriendshipAlreadyAcceptedException extends RuntimeException {

    public OfferFriendshipAlreadyAcceptedException(UUID offerFriendshipId, UUID fromUserId, UUID toUserId) {
        super("Offer friendship %s from user %s to user %s is already accepted"
                .formatted(offerFriendshipId, fromUserId, toUserId));
    }
}
