package ru.job4j.socialmediaapi.posts.internal.application.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Position of the last post of a feed page: the next page is read after it.
 *
 * @param createdAt creation time of the last post of the page
 * @param id        id of the last post of the page
 */
public record FeedCursorResult(
        Instant createdAt,
        UUID id
) {
}
