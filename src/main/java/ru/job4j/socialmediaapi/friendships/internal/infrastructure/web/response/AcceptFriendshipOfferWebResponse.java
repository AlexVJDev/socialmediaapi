package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response;

import java.time.Instant;
import java.util.UUID;

public record AcceptFriendshipOfferWebResponse(
        UUID id,
        UUID fromUserId,
        UUID toUserId,
        String status,
        Instant createdAt,
        Instant acceptedAt
) {
}
