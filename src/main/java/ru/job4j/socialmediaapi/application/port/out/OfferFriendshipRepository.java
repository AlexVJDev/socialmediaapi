package ru.job4j.socialmediaapi.application.port.out;

import ru.job4j.socialmediaapi.domain.offerfriendship.OfferFriendshipStatus;
import ru.job4j.socialmediaapi.domain.offerfriendship.exception.OfferFriendshipToSelfException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OfferFriendshipRepository {

    /**
     * @return the saved offer, or empty if an offer in the same direction already exists
     * @throws OfferFriendshipToSelfException the user offers friendship to themselves
     */
    Optional<CreateOfferFriendshipResponse> createOfferFriendshipIfAbsent(
            CreateOfferFriendshipRequest request
    );

    /**
     * @return the offer from {@code fromUserId} to {@code toUserId}, or empty if there is none
     */
    Optional<FindOfferFriendshipResponse> findByUsers(UUID fromUserId, UUID toUserId);

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

    record FindOfferFriendshipResponse(
            UUID id,
            UUID fromUserId,
            UUID toUserId,
            OfferFriendshipStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
