package ru.job4j.socialmediaapi.friendships.internal.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.job4j.socialmediaapi.friendships.internal.application.mapper.FriendshipOfferApplicationMapper;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipOfferRepository;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyAcceptedException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyRejectedException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipRequestNotFoundException;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOffer;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Chooses the error after {@link FriendshipOfferRepository#updateStatusIfPending} changed nothing:
 * the offer is read only now, to tell «not found» from «already decided» (the write itself
 * was decided by the {@code WHERE} of the update, not by this read). Which status gives which
 * error is the rule of the domain: the decision is replayed on the domain offer.
 * Shared by {@code AcceptFriendshipOffer} and {@code RejectFriendshipOffer}.
 */
@Component
@RequiredArgsConstructor
public class FriendshipOfferDecisionErrors {

    /** Reads the offer that could not be decided. */
    private final FriendshipOfferRepository repository;

    /** Restores the domain offer from the stored one. */
    private final FriendshipOfferApplicationMapper mapper;

    /**
     * @param offerId offer that could not be accepted
     * @param actorId user who tried to accept it
     * @param now     time of the attempt
     * @return {@link FriendshipRequestNotFoundException}, {@link FriendshipOfferAlreadyAcceptedException}
     *         or {@link FriendshipOfferAlreadyRejectedException}
     */
    public RuntimeException forAccept(UUID offerId, UUID actorId, Instant now) {
        return errorOf(offerId, actorId, offer -> offer.accept(now));
    }

    /**
     * @param offerId offer that could not be rejected
     * @param actorId user who tried to reject it
     * @param now     time of the attempt
     * @return {@link FriendshipRequestNotFoundException}, {@link FriendshipOfferAlreadyAcceptedException}
     *         or {@link FriendshipOfferAlreadyRejectedException}
     */
    public RuntimeException forReject(UUID offerId, UUID actorId, Instant now) {
        return errorOf(offerId, actorId, offer -> offer.reject(now));
    }

    /**
     * @param offerId  offer that could not be decided
     * @param actorId  user who tried to decide it
     * @param decision the decision replayed on the domain offer
     * @return the error to throw; {@link IllegalStateException} if the offer is still {@code PENDING},
     *         which a correct database cannot give right after an empty conditional update
     */
    private RuntimeException errorOf(UUID offerId, UUID actorId, Consumer<FriendshipOffer> decision) {
        var stored = repository.findByIdAndRecipient(offerId, actorId);
        if (stored.isEmpty()) {
            return new FriendshipRequestNotFoundException(offerId);
        }
        try {
            decision.accept(mapper.toDomain(stored.get()));
        } catch (FriendshipOfferAlreadyAcceptedException | FriendshipOfferAlreadyRejectedException e) {
            return e;
        }
        return new IllegalStateException("Offer %s is PENDING, but was not updated".formatted(offerId));
    }
}
