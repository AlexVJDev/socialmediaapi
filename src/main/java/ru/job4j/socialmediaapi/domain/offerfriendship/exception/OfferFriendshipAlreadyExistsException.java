package ru.job4j.socialmediaapi.domain.offerfriendship.exception;

import java.util.UUID;

public class OfferFriendshipAlreadyExistsException extends RuntimeException {

    public OfferFriendshipAlreadyExistsException(UUID fromUserId, UUID toUserId, Throwable cause) {
        super("Offer friendship from user %s to user %s already exists".formatted(fromUserId, toUserId), cause);
    }
}
