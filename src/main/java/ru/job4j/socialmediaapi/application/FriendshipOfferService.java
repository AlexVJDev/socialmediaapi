package ru.job4j.socialmediaapi.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.application.mapper.FriendshipOfferApplicationMapper;
import ru.job4j.socialmediaapi.application.port.in.CreateFriendshipOfferUseCase;
import ru.job4j.socialmediaapi.application.port.out.FriendshipOfferRepository;
import ru.job4j.socialmediaapi.application.port.out.FriendshipOfferRepository.CreateFriendshipOfferResponse;
import ru.job4j.socialmediaapi.application.port.out.FriendshipOfferRepository.FindFriendshipOfferResponse;
import ru.job4j.socialmediaapi.application.port.out.SubscriptionRepository;
import ru.job4j.socialmediaapi.application.port.out.SubscriptionRepository.CreateSubscriptionRequest;
import ru.job4j.socialmediaapi.domain.friendshipoffer.FriendshipOffer;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferAlreadyAcceptedException;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferAlreadyExistsException;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferAlreadyRejectedException;

import java.time.Clock;
import java.util.UUID;

/**
 * Creating an offer friendship, see «1. Создание заявки»
 * in {@code docs/process/friendship_transitions.puml}.
 *
 * <p>Concurrent creation of the same offer: {@code SELECT ... FOR UPDATE} cannot lock
 * a row that does not exist yet, so the losing transaction's insert waits for the winner
 * and fails with {@link FriendshipOfferAlreadyExistsException}. The repository rolls back
 * only that insert (a savepoint), so the transaction goes on: the service re-reads
 * the committed offer and answers as a repeat ({@code AlreadyPending} or a conflict).
 * The sender's subscription is inserted with {@code ON CONFLICT DO NOTHING}, so
 * a subscription created concurrently (e.g. by accepting the opposite offer)
 * is not an error.
 *
 * <p>The rules of a new offer (ids are set, no offer to oneself, {@code PENDING})
 * are checked by {@link FriendshipOffer#create} before the database is queried.
 */
@Service
public class FriendshipOfferService implements CreateFriendshipOfferUseCase {

    /** Stores friendship offers. */
    private final FriendshipOfferRepository friendshipOfferRepository;

    /** Stores the sender's subscription to the recipient.*/
    private final SubscriptionRepository subscriptionRepository;

    /** Maps commands and repository responses to use case results. */
    private final FriendshipOfferApplicationMapper mapper;

    /** Source of the creation time of offers and subscriptions. */
    private final Clock clock;

    public FriendshipOfferService(
            FriendshipOfferRepository friendshipOfferRepository,
            SubscriptionRepository subscriptionRepository,
            FriendshipOfferApplicationMapper mapper,
            Clock clock
    ) {
        this.friendshipOfferRepository = friendshipOfferRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CreateFriendshipOfferResult create(CreateFriendshipOfferCommand command) {
        var offer = FriendshipOffer.create(command.fromUserId(), command.toUserId(), clock.instant());
        return friendshipOfferRepository.findByUsers(offer.fromUserId(), offer.toUserId())
                .map(FriendshipOfferService::repeated)
                .orElseGet(() -> createNew(offer));
    }

    private static CreateFriendshipOfferResult repeated(FindFriendshipOfferResponse offer) {
        return switch (offer.status()) {
            case PENDING -> new CreateFriendshipOfferResult.AlreadyPending();
            case ACCEPTED -> throw new FriendshipOfferAlreadyAcceptedException(
                    offer.id(), offer.fromUserId(), offer.toUserId()
            );
            case REJECTED -> throw new FriendshipOfferAlreadyRejectedException(
                    offer.id(), offer.fromUserId(), offer.toUserId()
            );
        };
    }

    private CreateFriendshipOfferResult createNew(FriendshipOffer offer) {
        CreateFriendshipOfferResponse created;
        try {
            created = friendshipOfferRepository.createFriendshipOffer(mapper.toRequest(offer));
        } catch (FriendshipOfferAlreadyExistsException e) {
            return friendshipOfferRepository.findByUsers(offer.fromUserId(), offer.toUserId())
                    .map(FriendshipOfferService::repeated)
                    .orElseThrow(() -> new IllegalStateException(
                            "Offer friendship from user %s to user %s already exists but was not found"
                                    .formatted(offer.fromUserId(), offer.toUserId()), e
                    ));
        }
        subscriptionRepository.createSubscriptionIfAbsent(new CreateSubscriptionRequest(
                UUID.randomUUID(), offer.fromUserId(), offer.toUserId(), offer.createdAt()
        ));
        return mapper.toResult(created);
    }
}
