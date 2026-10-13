package ru.job4j.socialmediaapi.application.port.out;

import ru.job4j.socialmediaapi.domain.subscription.exception.SubscriptionAlreadyExistsException;
import ru.job4j.socialmediaapi.domain.subscription.exception.SubscriptionToSelfException;
import ru.job4j.socialmediaapi.domain.subscription.exception.SubscriptionUserRequiredException;

import java.time.Instant;
import java.util.UUID;

public interface SubscriptionRepository {

    /**
     * Saves a subscription of the follower to the followed user.
     *
     * @param request subscription to save
     * @return the saved subscription
     * @throws SubscriptionAlreadyExistsException the follower is already subscribed to the user
     * @throws SubscriptionToSelfException        the user subscribes to themselves
     * @throws SubscriptionUserRequiredException  the follower or followed user is not set
     */
    CreateSubscriptionResponse createSubscription(
            CreateSubscriptionRequest request
    );

    /**
     * @param followerId user who subscribes
     * @param followedId user being subscribed to
     * @return {@code true} if the follower is already subscribed to the followed user
     */
    boolean existsSubscription(UUID followerId, UUID followedId);

    record CreateSubscriptionRequest(
            UUID id,
            UUID followerId,
            UUID followedId,
            Instant createdAt
    ) {
    }

    record CreateSubscriptionResponse(
            UUID id,
            UUID followerId,
            UUID followedId,
            Instant createdAt
    ) {
    }
}
