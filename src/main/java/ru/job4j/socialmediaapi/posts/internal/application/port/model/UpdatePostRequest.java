package ru.job4j.socialmediaapi.posts.internal.application.port.model;

import java.time.Instant;
import java.util.UUID;

/**
 * New title and content of a post; applied only if {@code actorId} is its author.
 *
 * @param postId    post to change
 * @param actorId   user who changes the post
 * @param title     new title
 * @param content   new text
 * @param updatedAt time of the change
 */
public record UpdatePostRequest(
        UUID postId,
        UUID actorId,
        String title,
        String content,
        Instant updatedAt
) {
}
