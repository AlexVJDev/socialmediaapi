package ru.job4j.socialmediaapi.application.port.out;

import ru.job4j.socialmediaapi.domain.offerfriendship.OfferFriendshipStatus;
import ru.job4j.socialmediaapi.domain.offerfriendship.exception.OfferFriendshipAlreadyExistsException;
import ru.job4j.socialmediaapi.domain.offerfriendship.exception.OfferFriendshipToSelfException;

import java.time.Instant;
import java.util.UUID;

public interface OfferFriendshipRepository {

    /**
     * Saves an offer friendship from one user to another.
     *
     * @throws OfferFriendshipAlreadyExistsException an offer in the same direction already exists
     * @throws OfferFriendshipToSelfException        the user offers friendship to themselves
     */
    CreateOfferFriendshipResponse createOfferFriendship(
            CreateOfferFriendshipRequest request
    );

    record CreateOfferFriendshipRequest(
            UUID id,
            UUID fromUserId,
            UUID toUserId,
            OfferFriendshipStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    record CreateOfferFriendshipResponse(
            UUID id,
            UUID fromUserId,
            UUID toUserId,
            OfferFriendshipStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
