package ru.job4j.socialmediaapi.posts.internal.application.port.model;

import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Post to store: the caller sets the id, the status and the times.
 *
 * @param id        post id
 * @param authorId  author of the post (Keycloak user)
 * @param title     title
 * @param content   text of the post
 * @param status    status of the new post
 * @param createdAt time of creation
 * @param updatedAt time of the last change
 */
public record CreatePostRequest(
        UUID id,
        UUID authorId,
        String title,
        String content,
        PostStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
