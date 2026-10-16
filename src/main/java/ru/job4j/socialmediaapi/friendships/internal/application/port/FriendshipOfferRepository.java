package ru.job4j.socialmediaapi.friendships.internal.application.port;

import ru.job4j.socialmediaapi.friendships.internal.application.exception.FriendshipOfferAlreadyExistsException;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FindFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.UpdateFriendshipOfferStatusRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.UpdateFriendshipOfferStatusResponse;

import java.util.Optional;
import java.util.UUID;

public interface FriendshipOfferRepository {

    /**
     * Saves the offer. A pair of users has at most one offer in either direction,
     * so a repeated or a counter offer is a duplicate. A duplicate does not abort the caller's
     * transaction: only the failed insert is rolled back, so the caller may read the existing offer.
     *
     * @param request offer to save
     * @return the saved offer
     * @throws FriendshipOfferAlreadyExistsException an offer for this pair of users already exists
     *                                               (in the same or the opposite direction)
     */
    CreateFriendshipOfferResponse createFriendshipOffer(
            CreateFriendshipOfferRequest request
    );

    /**
     * Finds the offer of a pair of users in either direction and locks it until the end
     * of the transaction.
     *
     * @param userA one user of the pair
     * @param userB the other user of the pair
     * @return the offer from {@code userA} to {@code userB} or from {@code userB} to {@code userA};
     *         empty if the pair has no offer
     */
    Optional<FindFriendshipOfferResponse> findByPair(UUID userA, UUID userB);

    /**
     * Stores the decision on the offer in one statement: the status changes only if the offer
     * is still {@code PENDING} and {@code request.actorId()} is its recipient. Of concurrent
     * decisions on the same offer exactly one succeeds, the others get an empty result.
     *
     * @param request offer, user who decides, new status and time of the decision
     * @return the offer with the new status; empty if there is no such offer, the user is not
     *         its recipient or the offer is no longer {@code PENDING} (these cases are not told apart:
     *         to choose the error, read the offer with {@link #findByIdAndRecipient})
     */
    Optional<UpdateFriendshipOfferStatusResponse> updateStatusIfPending(UpdateFriendshipOfferStatusRequest request);

    /**
     * Reads the offer to choose the error after {@link #updateStatusIfPending} returned empty;
     * must not be used to decide whether to write.
     *
     * @param offerId     offer id
     * @param recipientId expected recipient of the offer
     * @return the offer; empty if there is no such offer or its recipient is not {@code recipientId}
     */
    Optional<FindFriendshipOfferResponse> findByIdAndRecipient(UUID offerId, UUID recipientId);
}
