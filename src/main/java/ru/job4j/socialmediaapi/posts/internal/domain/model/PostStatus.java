package ru.job4j.socialmediaapi.posts.internal.domain.model;

public enum PostStatus {
    /** The post is visible: it can be read and shows up in the feed. */
    PUBLISHED,
    /** The author deleted the post; the row is kept, but the post is not shown anywhere. */
    DELETED
}
