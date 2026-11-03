package ru.job4j.socialmediaapi.posts.internal.infrastructure.web.response;

import java.time.Instant;
import java.util.UUID;

public record PostWebResponse(
        UUID id,
        UUID authorId,
        String title,
        String content,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
