package ru.job4j.socialmediaapi.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.application.mapper.FriendshipOfferApplicationMapper;
import ru.job4j.socialmediaapi.application.port.in.CreateFriendshipOfferUseCase;
import ru.job4j.socialmediaapi.application.port.out.FriendshipOfferRepository;
import ru.job4j.socialmediaapi.application.port.out.FriendshipOfferRepository.CreateFriendshipOfferRequest;
import ru.job4j.socialmediaapi.application.port.out.FriendshipOfferRepository.FindFriendshipOfferResponse;
import ru.job4j.socialmediaapi.application.port.out.SubscriptionRepository;
import ru.job4j.socialmediaapi.application.port.out.SubscriptionRepository.CreateSubscriptionRequest;
import ru.job4j.socialmediaapi.domain.friendshipoffer.FriendshipOfferStatus;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferAlreadyAcceptedException;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferAlreadyRejectedException;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferToSelfException;

import java.time.Clock;
import java.util.UUID;

/**
 * Creating an offer friendship, see «1. Создание заявки»
 * in {@code docs/process/friendship_transitions.puml}.
 *
 * <p>Concurrent creation of the same offer: {@code SELECT ... FOR UPDATE} cannot lock
 * a row that does not exist yet, so the offer is inserted with
 * {@code ON CONFLICT DO NOTHING}. The losing transaction waits for the winner,
 * gets nothing inserted, re-reads the committed offer and answers as a repeat
 * ({@code AlreadyPending} or a conflict). The PostgreSQL transaction is never
 * aborted by a unique violation, so no retry outside the transaction is needed.
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
        var fromUserId = command.fromUserId();
        var toUserId = command.toUserId();
        if (fromUserId.equals(toUserId)) {
            throw new FriendshipOfferToSelfException(fromUserId);
        }
        return friendshipOfferRepository.findByUsers(fromUserId, toUserId)
                .map(FriendshipOfferService::repeated)
                .orElseGet(() -> createNew(fromUserId, toUserId));
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

    private CreateFriendshipOfferResult createNew(UUID fromUserId, UUID toUserId) {
        var now = clock.instant();
        var created = friendshipOfferRepository.createFriendshipOfferIfAbsent(new CreateFriendshipOfferRequest(
                UUID.randomUUID(), fromUserId, toUserId, FriendshipOfferStatus.PENDING, now, now
        ));
        if (created.isEmpty()) {
            return friendshipOfferRepository.findByUsers(fromUserId, toUserId)
                    .map(FriendshipOfferService::repeated)
                    .orElseThrow(() -> new IllegalStateException(
                            "Offer friendship from user %s to user %s was neither created nor found"
                                    .formatted(fromUserId, toUserId)
                    ));
        }
        if (!subscriptionRepository.existsSubscription(fromUserId, toUserId)) {
            subscriptionRepository.createSubscription(new CreateSubscriptionRequest(
                    UUID.randomUUID(), fromUserId, toUserId, now
            ));
        }
        return mapper.toResult(created.get());
    }
}
