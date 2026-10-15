package ru.job4j.socialmediaapi.application.port.in;

import ru.job4j.socialmediaapi.domain.friendshipoffer.FriendshipOfferStatus;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferAlreadyAcceptedException;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferAlreadyPendingException;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferAlreadyRejectedException;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferToSelfException;

import java.time.Instant;
import java.util.UUID;

public interface CreateFriendshipOfferUseCase {

    /**
     * Creates a {@code PENDING} offer friendship and subscribes the sender
     * to the recipient, if the sender is not subscribed yet.
     *
     * @param useCaseRequest sender and recipient of the offer
     * @return the created offer
     * @throws FriendshipOfferToSelfException          the user offers friendship to themselves
     * @throws FriendshipOfferAlreadyPendingException  the offer in the same direction already waits for a decision
     * @throws FriendshipOfferAlreadyAcceptedException the offer in the same direction is already accepted
     * @throws FriendshipOfferAlreadyRejectedException the offer in the same direction is already rejected
     */
    CreateFriendshipOfferResult create(CreateFriendshipOfferUseCaseRequest useCaseRequest);

    record CreateFriendshipOfferUseCaseRequest(
            UUID fromUserId,
            UUID toUserId
    ) {
    }

    record CreateFriendshipOfferResult(
            UUID id,
            UUID fromUserId,
            UUID toUserId,
            FriendshipOfferStatus status,
            Instant createdAt
    ) {
    }
}
