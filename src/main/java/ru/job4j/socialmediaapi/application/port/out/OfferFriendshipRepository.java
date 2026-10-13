package ru.job4j.socialmediaapi.application.port.out;

import ru.job4j.socialmediaapi.domain.offerfriendship.OfferFriendshipStatus;
import ru.job4j.socialmediaapi.domain.offerfriendship.exception.OfferFriendshipToSelfException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OfferFriendshipRepository {

    /**
     * @param request offer to save
     * @return the saved offer, or empty if an offer in the same direction already exists
     * @throws OfferFriendshipToSelfException the user offers friendship to themselves
     */
    Optional<CreateOfferFriendshipResponse> createOfferFriendshipIfAbsent(
            CreateOfferFriendshipRequest request
    );

    /**
     * @param fromUserId sender of the offer
     * @param toUserId   recipient of the offer
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
