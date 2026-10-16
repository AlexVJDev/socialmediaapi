package ru.job4j.socialmediaapi.friendships.internal.application.port.model;

import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Offer to store: the caller sets the id, the status and the times.
 *
 * @param id         id
 * @param fromUserId sender of the offer
 * @param toUserId   recipient of the offer
 * @param status     status
 * @param createdAt  time of creation
 * @param updatedAt  time of the last change
 */
public record CreateFriendshipOfferRequest(
        UUID id,
        UUID fromUserId,
        UUID toUserId,
        FriendshipOfferStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
