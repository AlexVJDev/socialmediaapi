package ru.job4j.socialmediaapi.posts.internal.application.model;

import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Post returned by the scenarios: a single post and a post of a feed page.
 *
 * @param id        post id
 * @param authorId  author of the post
 * @param title     title
 * @param content   text of the post
 * @param status    status, always {@code PUBLISHED}: deleted posts are not returned
 * @param createdAt time of creation
 * @param updatedAt time of the last change
 */
public record PostResult(
        UUID id,
        UUID authorId,
        String title,
        String content,
        PostStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
