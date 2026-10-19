package ru.job4j.socialmediaapi.posts.internal.application.port.model;

import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Post as stored; the same for created, found and updated posts.
 *
 * @param id        post id
 * @param authorId  author of the post
 * @param title     title
 * @param content   text of the post
 * @param status    status
 * @param createdAt time of creation
 * @param updatedAt time of the last change
 */
public record PostResponse(
        UUID id,
        UUID authorId,
        String title,
        String content,
        PostStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
