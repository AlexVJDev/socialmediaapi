package ru.job4j.socialmediaapi.application.port.out;

import ru.job4j.socialmediaapi.domain.post.PostStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Builds the feed of a reader when it is read: there is no feed table,
 * the posts are selected from {@code subscriptions} and {@code posts}.
 * See {@code docs/process/Feed.md}.
 */
public interface FeedRepository {

    /**
     * Published posts of the authors the follower is subscribed to, newest first
     * ({@code created_at DESC, id DESC}). Deleted posts and posts of other authors
     * are not included; the follower's own posts are not either, since a user
     * cannot be subscribed to themselves.
     *
     * @param followerId reader of the feed
     * @param limit      maximum number of posts on the page, greater than zero
     * @param offset     number of posts to skip, not negative
     * @return the page of the feed, empty if there is nothing to show
     * @throws IllegalArgumentException {@code limit} is not positive or {@code offset} is negative
     */
    List<FeedPostResponse> findFeed(UUID followerId, int limit, int offset);

    record FeedPostResponse(
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
