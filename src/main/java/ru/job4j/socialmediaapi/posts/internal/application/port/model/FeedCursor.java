package ru.job4j.socialmediaapi.posts.internal.application.port.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Position of the last shown post in the feed: the next page starts strictly after it
 * in the feed order ({@code created_at DESC, id DESC}). Take it only from
 * {@link FeedPageResponse#next()}: {@code TIMESTAMPTZ} keeps microseconds, so a cursor built
 * from an {@link Instant} with nanoseconds may not match any stored post.
 *
 * @param createdAt creation time of the last shown post
 * @param id        id of the last shown post, breaks ties of {@code createdAt}
 */
public record FeedCursor(
        Instant createdAt,
        UUID id
) {

    /**
     * Rejects an incomplete cursor.
     *
     * @throws NullPointerException {@code createdAt} or {@code id} is {@code null}
     */
    public FeedCursor {
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(id, "id");
    }
}
