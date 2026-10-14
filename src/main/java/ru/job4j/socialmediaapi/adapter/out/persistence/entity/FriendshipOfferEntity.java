package ru.job4j.socialmediaapi.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.UUID;

public record FriendshipOfferEntity(
        UUID id,
        UUID fromUserId,
        UUID toUserId,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
