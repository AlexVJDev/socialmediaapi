package ru.job4j.socialmediaapi.posts.internal.application.port.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Logical deletion of a post; applied only if {@code actorId} is its author.
 *
 * @param postId    post to delete
 * @param actorId   user who deletes the post
 * @param deletedAt time of the deletion
 */
public record DeletePostRequest(
        UUID postId,
        UUID actorId,
        Instant deletedAt
) {
}
