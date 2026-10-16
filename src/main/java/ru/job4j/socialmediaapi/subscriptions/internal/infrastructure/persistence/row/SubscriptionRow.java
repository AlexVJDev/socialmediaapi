package ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.persistence.row;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionRow(
        UUID id,
        UUID followerId,
        UUID followedId,
        Instant createdAt
) {
}
