package ru.job4j.socialmediaapi.domain.offerfriendship.exception;

import java.util.UUID;

public class OfferFriendshipToSelfException extends RuntimeException {

    public OfferFriendshipToSelfException(UUID userId, Throwable cause) {
        super("User %s cannot offer friendship to themselves".formatted(userId), cause);
    }
}
