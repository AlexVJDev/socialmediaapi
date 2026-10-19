package ru.job4j.socialmediaapi.posts.internal.application.port.model;

import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Post on a page of the feed.
 *
 * @param id        post id
 * @param authorId  author of the post
 * @param title     title
 * @param content   text of the post
 * @param status    status, always {@code PUBLISHED} in the feed
 * @param createdAt time of creation
 * @param updatedAt time of the last change
 */
public record FeedPostResponse(
        UUID id,
        UUID authorId,
        String title,
        String content,
        PostStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
