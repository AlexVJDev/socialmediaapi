package ru.job4j.socialmediaapi.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.UUID;

public record FriendshipEntity(
        UUID id,
        UUID firstUserId,
        UUID secondUserId,
        UUID offerFriendshipId,
        Instant createdAt
) {
}
