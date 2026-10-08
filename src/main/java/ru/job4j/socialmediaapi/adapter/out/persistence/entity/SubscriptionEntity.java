package ru.job4j.socialmediaapi.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionEntity(
        UUID id,
        UUID followerId,
        UUID followedId,
        Instant createdAt
) {
}
