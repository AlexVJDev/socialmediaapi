package ru.job4j.socialmediaapi.friendships.internal.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.friendships.internal.application.mapper.FriendshipOfferApplicationMapper;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferDecisionResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.RejectFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipOfferRepository;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.UpdateFriendshipOfferStatusRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.service.FriendshipOfferDecisionErrors;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyAcceptedException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyRejectedException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipRequestNotFoundException;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

import java.time.Clock;

/**
 * Rejecting a friendship offer, see «3. Отклонение заявки»
 * in {@code docs/process/friendship_transitions.puml}.
 *
 * <p>Only the status changes: no friendship is created, the sender stays subscribed
 * to the recipient. The status is changed by one conditional update (the offer is {@code PENDING}
 * and the actor is its recipient); of concurrent decisions exactly one succeeds.
 */
@Service
@RequiredArgsConstructor
public class RejectFriendshipOffer {

    /** Stores friendship offers. */
    private final FriendshipOfferRepository friendshipOfferRepository;

    /** Chooses the error when the offer could not be rejected. */
    private final FriendshipOfferDecisionErrors decisionErrors;

    /** Maps the port response of the offer to the result. */
    private final FriendshipOfferApplicationMapper mapper;

    /** Source of the decision time. */
    private final Clock clock;

    /**
     * Rejects a {@code PENDING} offer.
     *
     * @param command offer and the user who rejects it
     * @return the rejected offer
     * @throws FriendshipRequestNotFoundException      there is no such offer or the user is not its recipient
     * @throws FriendshipOfferAlreadyAcceptedException the offer is already accepted
     * @throws FriendshipOfferAlreadyRejectedException the offer is already rejected
     */
    @Transactional
    public FriendshipOfferDecisionResult reject(RejectFriendshipOfferCommand command) {
        var now = clock.instant();
        var rejected = friendshipOfferRepository.updateStatusIfPending(new UpdateFriendshipOfferStatusRequest(
                command.offerId(), command.actorId(), FriendshipOfferStatus.REJECTED, now
        )).orElseThrow(() -> decisionErrors.forReject(command.offerId(), command.actorId(), now));
        return mapper.toResult(rejected);
    }
}
