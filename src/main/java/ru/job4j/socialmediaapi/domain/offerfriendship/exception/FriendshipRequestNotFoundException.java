package ru.job4j.socialmediaapi.domain.offerfriendship.exception;

import java.util.UUID;

public class FriendshipRequestNotFoundException extends RuntimeException {

    public FriendshipRequestNotFoundException(UUID offerFriendshipId, Throwable cause) {
        super("Offer friendship %s not found".formatted(offerFriendshipId), cause);
    }
}
