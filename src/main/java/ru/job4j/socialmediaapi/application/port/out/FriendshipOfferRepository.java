package ru.job4j.socialmediaapi.application.port.out;

import ru.job4j.socialmediaapi.domain.friendshipoffer.FriendshipOfferStatus;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferToSelfException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface FriendshipOfferRepository {

    /**
     * @param request offer to save
     * @return the saved offer, or empty if an offer in the same direction already exists
     * @throws FriendshipOfferToSelfException the user offers friendship to themselves
     */
    Optional<CreateFriendshipOfferResponse> createFriendshipOfferIfAbsent(
            CreateFriendshipOfferRequest request
    );

    /**
     * @param fromUserId sender of the offer
     * @param toUserId   recipient of the offer
     * @return the offer from {@code fromUserId} to {@code toUserId}, or empty if there is none
     */
    Optional<FindFriendshipOfferResponse> findByUsers(UUID fromUserId, UUID toUserId);

    record CreateFriendshipOfferRequest(
            UUID id,
            UUID fromUserId,
            UUID toUserId,
            FriendshipOfferStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    record CreateFriendshipOfferResponse(
            UUID id,
            UUID fromUserId,
            UUID toUserId,
            FriendshipOfferStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    record FindFriendshipOfferResponse(
            UUID id,
            UUID fromUserId,
            UUID toUserId,
            FriendshipOfferStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
