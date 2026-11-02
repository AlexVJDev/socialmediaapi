package ru.job4j.socialmediaapi.posts.internal.application.port;

import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedCursor;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedPageResponse;

import java.util.UUID;

/**
 * Builds the feed of a reader when it is read: there is no feed table,
 * the posts are selected from {@code subscriptions} and {@code posts}.
 * The feed contains only published posts of the authors the reader is subscribed to,
 * newest first ({@code created_at DESC, id DESC}). Deleted posts and posts of other authors
 * are not included; the reader's own posts are not either, since a user cannot be subscribed
 * to themselves. Pages are read by a cursor: the first page by {@link #findFeed}, every next one
 * by {@link #findFeedAfter} with {@link FeedPageResponse#next()} of the previous page.
 * See {@code docs/process/Feed.md}.
 */
public interface FeedRepository {

    /** Maximum page size: a page costs about subscriptions × limit index entries. */
    int MAX_LIMIT = 100;

    /**
     * First page of the feed.
     *
     * @param followerId reader of the feed
     * @param limit      maximum number of posts on the page, from 1 to {@link #MAX_LIMIT}
     * @return the page; its {@code next} is empty if the feed has no more posts,
     *         both {@code posts} and {@code next} are empty if the feed is empty
     * @throws IllegalArgumentException {@code limit} is less than 1 or greater than {@link #MAX_LIMIT}
     */
    FeedPageResponse findFeed(UUID followerId, int limit);

    /**
     * Page of the feed with the posts strictly after {@code after} in the feed order.
     * Posts published after the first page was read are newer than any cursor and do not
     * appear on the next pages; deleted posts and unsubscribing do not shift the pages.
     *
     * @param followerId reader of the feed
     * @param after      position of the last shown post, taken from {@link FeedPageResponse#next()}
     * @param limit      maximum number of posts on the page, from 1 to {@link #MAX_LIMIT}
     * @return the page; its {@code next} is empty if the feed has no more posts,
     *         both {@code posts} and {@code next} are empty if nothing is left after {@code after}
     * @throws IllegalArgumentException {@code limit} is less than 1 or greater than {@link #MAX_LIMIT}
     * @throws NullPointerException     {@code after} is {@code null}
     */
    FeedPageResponse findFeedAfter(UUID followerId, FeedCursor after, int limit);
}
