package ru.job4j.socialmediaapi.subscriptions.api;

import org.springframework.modulith.NamedInterface;

import java.time.Instant;
import java.util.UUID;

/**
 * Subscriptions of a follower to a followed user, for other modules.
 * Operations run in the caller's transaction, so a caller can make
 * a subscription part of its own atomic change (e.g. creating a friendship offer).
 */
@NamedInterface("api")
public interface Subscriptions {

    /**
     * Subscribes the follower to the followed user unless already subscribed;
     * a duplicate (including one created concurrently) is not an error
     * and does not abort the caller's transaction.
     *
     * @param followerId user who subscribes
     * @param followedId user being subscribed to
     * @param createdAt  time of the subscription
     * @return {@code true} if the subscription was created,
     *         {@code false} if it already existed
     */
    boolean subscribeIfAbsent(UUID followerId, UUID followedId, Instant createdAt);

    /**
     * Removes the subscription of the follower to the followed user. The subscription
     * of the opposite direction is kept. After that the follower may subscribe again.
     *
     * @param followerId user who unsubscribes
     * @param followedId user being unsubscribed from
     * @return {@code true} if the subscription existed and was removed,
     *         {@code false} if there was nothing to remove
     */
    boolean unsubscribe(UUID followerId, UUID followedId);
}
