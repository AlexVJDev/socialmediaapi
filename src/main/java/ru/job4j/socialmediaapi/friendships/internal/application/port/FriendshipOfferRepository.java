package ru.job4j.socialmediaapi.friendships.internal.application.port;

import ru.job4j.socialmediaapi.friendships.internal.application.exception.FriendshipOfferAlreadyExistsException;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FindFriendshipOfferResponse;

import java.util.Optional;
import java.util.UUID;

public interface FriendshipOfferRepository {

    /**
     * Saves the offer. A duplicate does not abort the caller's transaction:
     * only the failed insert is rolled back, so the caller may read the existing offer.
     *
     * @param request offer to save
     * @return the saved offer
     * @throws FriendshipOfferAlreadyExistsException an offer in the same direction already exists
     */
    CreateFriendshipOfferResponse createFriendshipOffer(
            CreateFriendshipOfferRequest request
    );

    /**
     * @param fromUserId sender of the offer
     * @param toUserId   recipient of the offer
     * @return the offer from {@code fromUserId} to {@code toUserId}, or empty if there is none
     */
    Optional<FindFriendshipOfferResponse> findByUsers(UUID fromUserId, UUID toUserId);
}
