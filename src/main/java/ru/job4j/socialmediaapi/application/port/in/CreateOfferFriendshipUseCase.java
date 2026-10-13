package ru.job4j.socialmediaapi.application.port.in;

import ru.job4j.socialmediaapi.domain.offerfriendship.OfferFriendshipStatus;
import ru.job4j.socialmediaapi.domain.offerfriendship.exception.OfferFriendshipAlreadyAcceptedException;
import ru.job4j.socialmediaapi.domain.offerfriendship.exception.OfferFriendshipAlreadyRejectedException;
import ru.job4j.socialmediaapi.domain.offerfriendship.exception.OfferFriendshipToSelfException;

import java.time.Instant;
import java.util.UUID;

public interface CreateOfferFriendshipUseCase {

    /**
     * Creates a {@code PENDING} offer friendship and subscribes the sender
     * to the recipient, if the sender is not subscribed yet.
     *
     * @return {@link CreateOfferFriendshipResult.Created} for a new offer,
     *         {@link CreateOfferFriendshipResult.AlreadyPending} if the offer already waits for a decision
     * @throws OfferFriendshipToSelfException          the user offers friendship to themselves
     * @throws OfferFriendshipAlreadyAcceptedException the offer in the same direction is already accepted
     * @throws OfferFriendshipAlreadyRejectedException the offer in the same direction is already rejected
     */
    CreateOfferFriendshipResult create(CreateOfferFriendshipCommand command);

    record CreateOfferFriendshipCommand(
            UUID fromUserId,
            UUID toUserId
    ) {
    }

    sealed interface CreateOfferFriendshipResult {

        record Created(
                UUID id,
                UUID fromUserId,
                UUID toUserId,
                OfferFriendshipStatus status,
                Instant createdAt
        ) implements CreateOfferFriendshipResult {
        }

        record AlreadyPending() implements CreateOfferFriendshipResult {
        }
    }
}
