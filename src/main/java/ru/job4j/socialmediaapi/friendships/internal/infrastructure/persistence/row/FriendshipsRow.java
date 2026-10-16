package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.row;

import java.time.Instant;
import java.util.UUID;

public record FriendshipsRow(
        UUID id,
        UUID firstUserId,
        UUID secondUserId,
        UUID friendshipOfferId,
        String status,
        Instant createdAt
) {
}
