package ru.job4j.socialmediaapi.domain.friendshipoffer;

import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferToSelfException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Friendship offer aggregate. Its invariants (all fields are set, an offer
 * to oneself is impossible) hold for every instance; the database constraints
 * of {@code friendship_offer} only back them up.
 *
 * @param id         offer id
 * @param fromUserId sender of the offer
 * @param toUserId   recipient of the offer
 * @param status     current status of the offer
 * @param createdAt  creation time
 * @param updatedAt  time of the last status change
 */
public record FriendshipOffer(
        UUID id,
        UUID fromUserId,
        UUID toUserId,
        FriendshipOfferStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public FriendshipOffer {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(fromUserId, "fromUserId");
        Objects.requireNonNull(toUserId, "toUserId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (fromUserId.equals(toUserId)) {
            throw new FriendshipOfferToSelfException(fromUserId);
        }
    }

    /**
     * Creates a new offer: it always starts as {@code PENDING}.
     *
     * @param fromUserId sender of the offer
     * @param toUserId   recipient of the offer
     * @param now        creation time
     * @return the new offer with a generated id
     * @throws FriendshipOfferToSelfException the user offers friendship to themselves
     */
    public static FriendshipOffer create(UUID fromUserId, UUID toUserId, Instant now) {
        return new FriendshipOffer(
                UUID.randomUUID(), fromUserId, toUserId, FriendshipOfferStatus.PENDING, now, now
        );
    }
}
