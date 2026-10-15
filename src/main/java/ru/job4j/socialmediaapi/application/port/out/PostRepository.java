package ru.job4j.socialmediaapi.application.port.out;

import ru.job4j.socialmediaapi.domain.post.PostStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores single posts. Changing and deleting a post is allowed only to its author:
 * the {@code actorId} of a request must match the post's {@code authorId}.
 *
 * <p>All methods return the same {@link PostResponse}: the created, found and
 * updated post have the same fields, so separate response records would only repeat it.
 */
public interface PostRepository {

    /**
     * @param request post to save
     * @return the saved post
     */
    PostResponse createPost(CreatePostRequest request);

    /**
     * @param postId id of the post
     * @return the post if it exists and is {@code PUBLISHED}; a deleted post is read as missing
     */
    Optional<PostResponse> findPublishedById(UUID postId);

    /**
     * Changes the title and the content of a published post of the actor.
     *
     * @param request new title and content, the post and the actor
     * @return the updated post, or empty if the post does not exist, is deleted
     *         or {@code actorId} is not its author
     */
    Optional<PostResponse> updatePost(UpdatePostRequest request);

    /**
     * Deletes a published post of the actor logically: the status becomes {@code DELETED}.
     *
     * @param request the post and the actor
     * @return {@code true} if the post was deleted; {@code false} if it does not exist,
     *         is already deleted or {@code actorId} is not its author
     */
    boolean deletePost(DeletePostRequest request);

    record CreatePostRequest(
            UUID id,
            UUID authorId,
            String title,
            String content,
            PostStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    record UpdatePostRequest(
            UUID postId,
            UUID actorId,
            String title,
            String content,
            Instant updatedAt
    ) {
    }

    record DeletePostRequest(
            UUID postId,
            UUID actorId,
            Instant deletedAt
    ) {
    }

    record PostResponse(
            UUID id,
            UUID authorId,
            String title,
            String content,
            PostStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
