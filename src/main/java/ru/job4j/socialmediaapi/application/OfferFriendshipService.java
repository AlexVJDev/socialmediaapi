package ru.job4j.socialmediaapi.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.application.mapper.OfferFriendshipApplicationMapper;
import ru.job4j.socialmediaapi.application.port.in.CreateOfferFriendshipUseCase;
import ru.job4j.socialmediaapi.application.port.out.OfferFriendshipRepository;
import ru.job4j.socialmediaapi.application.port.out.OfferFriendshipRepository.CreateOfferFriendshipRequest;
import ru.job4j.socialmediaapi.application.port.out.OfferFriendshipRepository.FindOfferFriendshipResponse;
import ru.job4j.socialmediaapi.application.port.out.SubscriptionRepository;
import ru.job4j.socialmediaapi.application.port.out.SubscriptionRepository.CreateSubscriptionRequest;
import ru.job4j.socialmediaapi.domain.offerfriendship.OfferFriendshipStatus;
import ru.job4j.socialmediaapi.domain.offerfriendship.exception.OfferFriendshipAlreadyAcceptedException;
import ru.job4j.socialmediaapi.domain.offerfriendship.exception.OfferFriendshipAlreadyRejectedException;
import ru.job4j.socialmediaapi.domain.offerfriendship.exception.OfferFriendshipToSelfException;

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
public class OfferFriendshipService implements CreateOfferFriendshipUseCase {

    private final OfferFriendshipRepository offerFriendshipRepository;

    private final SubscriptionRepository subscriptionRepository;

    private final OfferFriendshipApplicationMapper mapper;

    private final Clock clock;

    public OfferFriendshipService(
            OfferFriendshipRepository offerFriendshipRepository,
            SubscriptionRepository subscriptionRepository,
            OfferFriendshipApplicationMapper mapper,
            Clock clock
    ) {
        this.offerFriendshipRepository = offerFriendshipRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CreateOfferFriendshipResult create(CreateOfferFriendshipCommand command) {
        var fromUserId = command.fromUserId();
        var toUserId = command.toUserId();
        if (fromUserId.equals(toUserId)) {
            throw new OfferFriendshipToSelfException(fromUserId);
        }
        return offerFriendshipRepository.findByUsers(fromUserId, toUserId)
                .map(OfferFriendshipService::repeated)
                .orElseGet(() -> createNew(fromUserId, toUserId));
    }

    private static CreateOfferFriendshipResult repeated(FindOfferFriendshipResponse offer) {
        return switch (offer.status()) {
            case PENDING -> new CreateOfferFriendshipResult.AlreadyPending();
            case ACCEPTED -> throw new OfferFriendshipAlreadyAcceptedException(
                    offer.id(), offer.fromUserId(), offer.toUserId()
            );
            case REJECTED -> throw new OfferFriendshipAlreadyRejectedException(
                    offer.id(), offer.fromUserId(), offer.toUserId()
            );
        };
    }

    private CreateOfferFriendshipResult createNew(UUID fromUserId, UUID toUserId) {
        var now = clock.instant();
        var created = offerFriendshipRepository.createOfferFriendshipIfAbsent(new CreateOfferFriendshipRequest(
                UUID.randomUUID(), fromUserId, toUserId, OfferFriendshipStatus.PENDING, now, now
        ));
        if (created.isEmpty()) {
            return offerFriendshipRepository.findByUsers(fromUserId, toUserId)
                    .map(OfferFriendshipService::repeated)
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
