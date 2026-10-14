package ru.job4j.socialmediaapi.application.port.out;

import ru.job4j.socialmediaapi.domain.subscription.exception.SubscriptionAlreadyExistsException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository {

    /**
     * Saves a subscription of the follower to the followed user.
     *
     * @param request subscription to save
     * @return the saved subscription
     * @throws SubscriptionAlreadyExistsException the follower is already subscribed to the user
     */
    CreateSubscriptionResponse createSubscription(
            CreateSubscriptionRequest request
    );

    /**
     * Saves the subscription unless the follower is already subscribed to the user;
     * a duplicate is reported by an empty result, so a concurrent insert
     * does not abort the caller's transaction.
     *
     * @param request subscription to save
     * @return the saved subscription, or empty if it already exists
     */
    Optional<CreateSubscriptionResponse> createSubscriptionIfAbsent(
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
