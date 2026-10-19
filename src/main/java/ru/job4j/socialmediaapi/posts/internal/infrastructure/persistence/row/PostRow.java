package ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.row;

import java.time.Instant;
import java.util.UUID;

public record PostRow(
        UUID id,
        UUID authorId,
        String title,
        String content,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
