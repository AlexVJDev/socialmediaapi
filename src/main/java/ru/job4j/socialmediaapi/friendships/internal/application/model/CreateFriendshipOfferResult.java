package ru.job4j.socialmediaapi.friendships.internal.application.model;

import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Result of the scenario «create a friendship offer»: the created offer.
 *
 * @param id         offer id
 * @param fromUserId sender of the offer
 * @param toUserId   recipient of the offer
 * @param status     status of the offer, {@code PENDING} for a new one
 * @param createdAt  time of creation
 */
public record CreateFriendshipOfferResult(
        UUID id,
        UUID fromUserId,
        UUID toUserId,
        FriendshipOfferStatus status,
        Instant createdAt
) {
}
