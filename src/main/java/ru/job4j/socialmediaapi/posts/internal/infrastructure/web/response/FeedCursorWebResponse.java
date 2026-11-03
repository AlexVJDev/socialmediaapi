package ru.job4j.socialmediaapi.posts.internal.infrastructure.web.response;

import java.time.Instant;
import java.util.UUID;

public record FeedCursorWebResponse(
        Instant createdAt,
        UUID id
) {
}
