package ru.job4j.socialmediaapi.subscriptions.internal.application.port.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Subscription to store: the caller sets the id and the creation time.
 *
 * @param id         subscription id
 * @param followerId user who subscribes
 * @param followedId user being subscribed to
 * @param createdAt  time of the subscription
 */
public record CreateSubscriptionRequest(
        UUID id,
        UUID followerId,
        UUID followedId,
        Instant createdAt
) {
}
