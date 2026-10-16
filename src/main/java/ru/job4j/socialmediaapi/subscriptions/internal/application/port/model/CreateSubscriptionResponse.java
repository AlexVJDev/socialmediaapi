package ru.job4j.socialmediaapi.subscriptions.internal.application.port.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Subscription as stored in the database.
 *
 * @param id         subscription id
 * @param followerId user who subscribes
 * @param followedId user being subscribed to
 * @param createdAt  time of the subscription
 */
public record CreateSubscriptionResponse(
        UUID id,
        UUID followerId,
        UUID followedId,
        Instant createdAt
) {
}
