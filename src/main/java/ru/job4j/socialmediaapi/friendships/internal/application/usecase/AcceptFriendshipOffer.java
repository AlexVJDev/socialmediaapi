package ru.job4j.socialmediaapi.friendships.internal.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.friendships.internal.application.mapper.FriendshipApplicationMapper;
import ru.job4j.socialmediaapi.friendships.internal.application.mapper.FriendshipOfferApplicationMapper;
import ru.job4j.socialmediaapi.friendships.internal.application.model.AcceptFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferDecisionResult;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipOfferRepository;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipsRepository;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.UpdateFriendshipOfferStatusRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.service.FriendshipOfferDecisionErrors;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyAcceptedException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyRejectedException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipRequestNotFoundException;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.Friendship;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;
import ru.job4j.socialmediaapi.subscriptions.api.Subscriptions;

import java.time.Clock;

/**
 * Accepting a friendship offer, see «2. Принятие заявки»
 * in {@code docs/process/friendship_transitions.puml}.
 *
 * <p>One transaction: the status becomes {@code ACCEPTED}, the friendship of the pair is created
 * and the recipient subscribes to the sender (if not subscribed yet) — all or nothing.
 *
 * <p>The status is changed first, by one conditional update (the offer is {@code PENDING} and
 * the actor is its recipient), without a preliminary lookup. Of a concurrent accept and reject
 * (or two accepts) exactly one changes the status; the other finds nothing to update and gets
 * the error of the status the winner has set.
 */
@Service
@RequiredArgsConstructor
public class AcceptFriendshipOffer {

    /** Stores friendship offers. */
    private final FriendshipOfferRepository friendshipOfferRepository;

    /** Stores friendships. */
    private final FriendshipsRepository friendshipsRepository;

    /** Subscribes the recipient to the sender (API of the subscriptions module). */
    private final Subscriptions subscriptions;

    /** Chooses the error when the offer could not be accepted. */
    private final FriendshipOfferDecisionErrors decisionErrors;

    /** Maps the port response of the offer to the result. */
    private final FriendshipOfferApplicationMapper offerMapper;

    /** Maps the domain friendship to the port request. */
    private final FriendshipApplicationMapper friendshipMapper;

    /** Source of the decision time. */
    private final Clock clock;

    /**
     * Accepts a {@code PENDING} offer: friendship and the back subscription are created atomically.
     *
     * @param command offer and the user who accepts it
     * @return the accepted offer
     * @throws FriendshipRequestNotFoundException      there is no such offer or the user is not its recipient
     * @throws FriendshipOfferAlreadyAcceptedException the offer is already accepted
     * @throws FriendshipOfferAlreadyRejectedException the offer is already rejected
     */
    @Transactional
    public FriendshipOfferDecisionResult accept(AcceptFriendshipOfferCommand command) {
        var now = clock.instant();
        var accepted = friendshipOfferRepository.updateStatusIfPending(new UpdateFriendshipOfferStatusRequest(
                command.offerId(), command.actorId(), FriendshipOfferStatus.ACCEPTED, now
        )).orElseThrow(() -> decisionErrors.forAccept(command.offerId(), command.actorId(), now));
        var friendship = Friendship.between(accepted.fromUserId(), accepted.toUserId(), accepted.id(), now);
        friendshipsRepository.createFriendship(friendshipMapper.toRequest(friendship));
        subscriptions.subscribeIfAbsent(accepted.toUserId(), accepted.fromUserId(), now);
        return offerMapper.toResult(accepted);
    }
}
