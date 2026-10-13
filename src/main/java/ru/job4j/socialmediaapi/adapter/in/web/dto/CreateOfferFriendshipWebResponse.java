package ru.job4j.socialmediaapi.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

public record CreateOfferFriendshipWebResponse(
        UUID id,
        UUID fromUserId,
        UUID toUserId,
        String status,
        Instant createdAt
) {
}
