package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.row;

import java.time.Instant;
import java.util.UUID;

public record FriendshipOfferRow(
        UUID id,
        UUID fromUserId,
        UUID toUserId,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
