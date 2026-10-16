package ru.job4j.socialmediaapi.friendships.internal.domain.model;

import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyAcceptedException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyRejectedException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferToSelfException;

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

    /**
     * Accepts the offer: {@code PENDING → ACCEPTED}.
     *
     * @param now time of the decision
     * @return the accepted offer; this one is not changed
     * @throws FriendshipOfferAlreadyAcceptedException the offer is already accepted
     * @throws FriendshipOfferAlreadyRejectedException the offer is already rejected
     */
    public FriendshipOffer accept(Instant now) {
        return decide(FriendshipOfferStatus.ACCEPTED, now);
    }

    /**
     * Rejects the offer: {@code PENDING → REJECTED}.
     *
     * @param now time of the decision
     * @return the rejected offer; this one is not changed
     * @throws FriendshipOfferAlreadyAcceptedException the offer is already accepted
     * @throws FriendshipOfferAlreadyRejectedException the offer is already rejected
     */
    public FriendshipOffer reject(Instant now) {
        return decide(FriendshipOfferStatus.REJECTED, now);
    }

    /**
     * Only a {@code PENDING} offer can be decided: {@code ACCEPTED} and {@code REJECTED} are final.
     *
     * @param decision new status
     * @param now      time of the decision
     * @return the offer with the new status and {@code updatedAt = now}
     */
    private FriendshipOffer decide(FriendshipOfferStatus decision, Instant now) {
        return switch (status) {
            case PENDING -> new FriendshipOffer(id, fromUserId, toUserId, decision, createdAt, now);
            case ACCEPTED -> throw new FriendshipOfferAlreadyAcceptedException(id, fromUserId, toUserId);
            case REJECTED -> throw new FriendshipOfferAlreadyRejectedException(id, fromUserId, toUserId);
        };
    }
}
