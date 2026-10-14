package ru.job4j.socialmediaapi.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.UUID;

public record FriendshipsEntity(
        UUID id,
        UUID firstUserId,
        UUID secondUserId,
        UUID friendshipOfferId,
        String status,
        Instant createdAt
) {
}
