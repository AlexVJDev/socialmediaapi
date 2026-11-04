package ru.job4j.socialmediaapi.friendships.internal.application.model;

import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Friendship offer as read by the scenarios «get a friendship offer» and
 * «get a page of all friendship offers».
 *
 * @param id         offer id
 * @param fromUserId sender of the offer
 * @param toUserId   recipient of the offer
 * @param status     status
 * @param createdAt  time of creation
 * @param updatedAt  time of the last change (of the decision, for a decided offer)
 */
public record FriendshipOfferResult(
        UUID id,
        UUID fromUserId,
        UUID toUserId,
        FriendshipOfferStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
