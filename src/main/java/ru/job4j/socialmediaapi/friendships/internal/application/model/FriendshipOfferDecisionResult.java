package ru.job4j.socialmediaapi.friendships.internal.application.model;

import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Result of the scenarios «accept a friendship offer» and «reject a friendship offer»:
 * the decided offer. One result for both scenarios, because their fields are the same;
 * the web layer names {@code decidedAt} {@code acceptedAt} or {@code rejectedAt}.
 *
 * @param id         offer id
 * @param fromUserId sender of the offer
 * @param toUserId   recipient of the offer, who decided it
 * @param status     new status: {@code ACCEPTED} or {@code REJECTED}
 * @param createdAt  time of creation
 * @param decidedAt  time of the decision ({@code updated_at} of the offer)
 */
public record FriendshipOfferDecisionResult(
        UUID id,
        UUID fromUserId,
        UUID toUserId,
        FriendshipOfferStatus status,
        Instant createdAt,
        Instant decidedAt
) {
}
