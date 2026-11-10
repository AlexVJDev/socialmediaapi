package ru.job4j.socialmediaapi.subscriptions.internal.application.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Subscription returned by the reading scenarios.
 *
 * @param id         subscription id
 * @param followerId user who is subscribed
 * @param followedId user being subscribed to
 * @param createdAt  time of the subscription
 */
public record SubscriptionResult(
        UUID id,
        UUID followerId,
        UUID followedId,
        Instant createdAt
) {
}
