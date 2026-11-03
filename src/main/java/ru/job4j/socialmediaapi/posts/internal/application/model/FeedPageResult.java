package ru.job4j.socialmediaapi.posts.internal.application.model;

import java.util.List;

/**
 * Result of the scenario «get a page of the feed».
 *
 * @param posts posts of the page, newest first
 * @param next  cursor of the next page; {@code null} if there are no more posts
 */
public record FeedPageResult(
        List<PostResult> posts,
        FeedCursorResult next
) {
}
