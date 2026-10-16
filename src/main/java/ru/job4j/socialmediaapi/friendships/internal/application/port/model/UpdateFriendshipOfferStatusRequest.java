package ru.job4j.socialmediaapi.friendships.internal.application.port.model;

import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Decision on a {@code PENDING} offer: the new status is stored only if the offer is still
 * {@code PENDING} and {@code actorId} is its recipient.
 *
 * @param offerId   offer to decide
 * @param actorId   user who decides; must be the recipient of the offer
 * @param status    new status ({@code ACCEPTED} or {@code REJECTED})
 * @param updatedAt time of the decision
 */
public record UpdateFriendshipOfferStatusRequest(
        UUID offerId,
        UUID actorId,
        FriendshipOfferStatus status,
        Instant updatedAt
) {
}
