package ru.job4j.socialmediaapi.friendships.internal.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.friendships.internal.application.mapper.FriendshipOfferApplicationMapper;
import ru.job4j.socialmediaapi.friendships.internal.application.model.CreateFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.model.CreateFriendshipOfferResult;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipOfferRepository;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferResponse;
import ru.job4j.socialmediaapi.subscriptions.api.Subscriptions;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOffer;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyAcceptedException;
import ru.job4j.socialmediaapi.friendships.internal.application.exception.FriendshipOfferAlreadyExistsException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyPendingException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyRejectedException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferToSelfException;

import java.time.Clock;

/**
 * Creating an offer friendship, see «1. Создание заявки»
 * in {@code docs/process/friendship_transitions.puml}.
 *
 * <p>The offer is inserted right away, without a preliminary lookup: the uniqueness
 * of an offer per pair of users is guarded by the unique key of the table, so a repeat
 * (sequential or concurrent) fails with {@link FriendshipOfferAlreadyExistsException}.
 * The repository rolls back only that insert (a savepoint), so the service can read
 * the existing offer and throw the error of its status: {@link FriendshipOfferAlreadyPendingException},
 * {@link FriendshipOfferAlreadyAcceptedException} or {@link FriendshipOfferAlreadyRejectedException};
 * the transaction is then rolled back. The sender's subscription is inserted with
 * {@code ON CONFLICT DO NOTHING}, so a subscription created concurrently
 * (e.g. by accepting the opposite offer) is not an error.
 *
 * <p>The rules of a new offer (ids are set, no offer to oneself, {@code PENDING})
 * are checked by {@link FriendshipOffer#create} before the database is queried.
 */
@Service
@RequiredArgsConstructor
public class CreateFriendshipOffer {

    /** Stores friendship offers. */
    private final FriendshipOfferRepository friendshipOfferRepository;

    /** Subscribes the sender to the recipient (API of the subscriptions module). */
    private final Subscriptions subscriptions;

    /** Maps the domain offer to the port request and the port response to the result. */
    private final FriendshipOfferApplicationMapper mapper;

    /** Source of the creation time of offers and subscriptions. */
    private final Clock clock;

    /**
     * Creates a {@code PENDING} offer friendship and subscribes the sender
     * to the recipient, if the sender is not subscribed yet.
     *
     * @param command sender and recipient of the offer
     * @return the created offer
     * @throws FriendshipOfferToSelfException          the user offers friendship to themselves
     * @throws FriendshipOfferAlreadyPendingException  the offer in the same direction already waits for a decision
     * @throws FriendshipOfferAlreadyAcceptedException the offer in the same direction is already accepted
     * @throws FriendshipOfferAlreadyRejectedException the offer in the same direction is already rejected
     */
    @Transactional
    public CreateFriendshipOfferResult create(CreateFriendshipOfferCommand command) {
        var offer = FriendshipOffer.create(command.fromUserId(), command.toUserId(), clock.instant());
        CreateFriendshipOfferResponse created;
        try {
            created = friendshipOfferRepository.createFriendshipOffer(mapper.toRequest(offer));
        } catch (FriendshipOfferAlreadyExistsException e) {
            throw alreadyExistsError(offer, e);
        }
        subscriptions.subscribeIfAbsent(offer.fromUserId(), offer.toUserId(), offer.createdAt());
        return mapper.toResult(created);
    }

    /**
     * Reads the offer that blocked the insert and picks the error of its status;
     * if the offer cannot be read, the original duplicate error is returned.
     *
     * @param offer offer that could not be inserted
     * @param cause unique key violation reported by the repository
     * @return the error to throw
     */
    private RuntimeException alreadyExistsError(
            FriendshipOffer offer, FriendshipOfferAlreadyExistsException cause
    ) {
        return friendshipOfferRepository.findByUsers(offer.fromUserId(), offer.toUserId())
                .map(existing -> switch (existing.status()) {
                    case PENDING -> new FriendshipOfferAlreadyPendingException(
                            existing.id(), existing.fromUserId(), existing.toUserId()
                    );
                    case ACCEPTED -> new FriendshipOfferAlreadyAcceptedException(
                            existing.id(), existing.fromUserId(), existing.toUserId()
                    );
                    case REJECTED -> new FriendshipOfferAlreadyRejectedException(
                            existing.id(), existing.fromUserId(), existing.toUserId()
                    );
                })
                .orElse(cause);
    }
}
