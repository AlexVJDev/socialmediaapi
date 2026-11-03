package ru.job4j.socialmediaapi.posts.internal.application.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Command of the scenario «get a page of the feed». Without a cursor
 * ({@code afterId == null}) the first page is read, otherwise the posts strictly after
 * the cursor; the cursor is taken from {@link FeedPageResult#next()} of the previous page.
 *
 * @param followerId     reader of the feed
 * @param afterCreatedAt creation time of the last shown post, {@code null} for the first page
 * @param afterId        id of the last shown post, {@code null} for the first page
 * @param limit          maximum number of posts on the page
 */
public record GetFeedCommand(
        UUID followerId,
        Instant afterCreatedAt,
        UUID afterId,
        int limit
) {
}
