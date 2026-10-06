package ru.job4j.socialmediaapi.repository;

import java.time.Instant;
import java.util.UUID;

record OfferFriendshipEntity(
        UUID id,
        UUID fromUserId,
        UUID toUserId,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
