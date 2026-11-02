package ru.job4j.socialmediaapi.posts.internal.application.port.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Page of the feed.
 *
 * @param posts posts of the page, newest first, not more than the requested limit; unmodifiable
 * @param next  cursor of the next page (the position of the last post of {@code posts});
 *              empty if there are no more posts
 */
public record FeedPageResponse(
        List<FeedPostResponse> posts,
        Optional<FeedCursor> next
) {

    /**
     * Copies the posts into an unmodifiable list.
     *
     * @throws NullPointerException {@code posts}, one of the posts or {@code next} is {@code null}
     */
    public FeedPageResponse {
        posts = List.copyOf(posts);
        Objects.requireNonNull(next, "next");
    }
}
