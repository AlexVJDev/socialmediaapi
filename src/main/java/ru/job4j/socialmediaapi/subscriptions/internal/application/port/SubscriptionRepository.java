package ru.job4j.socialmediaapi.subscriptions.internal.application.port;

import ru.job4j.socialmediaapi.subscriptions.internal.application.port.model.CreateSubscriptionRequest;
import ru.job4j.socialmediaapi.subscriptions.internal.application.port.model.CreateSubscriptionResponse;
import ru.job4j.socialmediaapi.subscriptions.internal.application.exception.SubscriptionAlreadyExistsException;

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
     * Only for reading the current state. Do not call it as a check before
     * {@link #createSubscription} or {@link #deleteSubscription}: the answer may be
     * out of date by the next statement (check-then-act); those methods are atomic themselves.
     *
     * @param followerId user who subscribes
     * @param followedId user being subscribed to
     * @return {@code true} if the follower is already subscribed to the followed user
     */
    boolean existsSubscription(UUID followerId, UUID followedId);

    /**
     * Removes the subscription of the follower to the followed user. The subscription
     * of the opposite direction is kept. After that the follower may subscribe again.
     *
     * @param followerId user who unsubscribes
     * @param followedId user being unsubscribed from
     * @return {@code true} if the subscription existed and was removed,
     *         {@code false} if there was nothing to remove
     */
    boolean deleteSubscription(UUID followerId, UUID followedId);
}
