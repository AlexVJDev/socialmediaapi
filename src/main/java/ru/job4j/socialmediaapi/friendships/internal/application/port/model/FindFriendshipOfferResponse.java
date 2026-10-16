package ru.job4j.socialmediaapi.friendships.internal.application.port.model;

import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Offer found by its sender and recipient.
 *
 * @param id         id
 * @param fromUserId sender of the offer
 * @param toUserId   recipient of the offer
 * @param status     status
 * @param createdAt  time of creation
 * @param updatedAt  time of the last change
 */
public record FindFriendshipOfferResponse(
        UUID id,
        UUID fromUserId,
        UUID toUserId,
        FriendshipOfferStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
