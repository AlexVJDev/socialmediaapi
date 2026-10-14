package ru.job4j.socialmediaapi.application.port.in;

import ru.job4j.socialmediaapi.domain.friendshipoffer.FriendshipOfferStatus;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferAlreadyAcceptedException;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferAlreadyRejectedException;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferToSelfException;

import java.time.Instant;
import java.util.UUID;

public interface CreateFriendshipOfferUseCase {

    /**
     * Creates a {@code PENDING} offer friendship and subscribes the sender
     * to the recipient, if the sender is not subscribed yet.
     *
     * @param command sender and recipient of the offer
     * @return {@link CreateFriendshipOfferResult.Created} for a new offer,
     *         {@link CreateFriendshipOfferResult.AlreadyPending} if the offer already waits for a decision
     * @throws FriendshipOfferToSelfException          the user offers friendship to themselves
     * @throws FriendshipOfferAlreadyAcceptedException the offer in the same direction is already accepted
     * @throws FriendshipOfferAlreadyRejectedException the offer in the same direction is already rejected
     */
    CreateFriendshipOfferResult create(CreateFriendshipOfferCommand command);

    record CreateFriendshipOfferCommand(
            UUID fromUserId,
            UUID toUserId
    ) {
    }

    sealed interface CreateFriendshipOfferResult {

        record Created(
                UUID id,
                UUID fromUserId,
                UUID toUserId,
                FriendshipOfferStatus status,
                Instant createdAt
        ) implements CreateFriendshipOfferResult {
        }

        record AlreadyPending() implements CreateFriendshipOfferResult {
        }
    }
}
