package ru.job4j.socialmediaapi.posts.internal.domain.exception;

import java.util.UUID;

/**
 * The post is not found: there is no such post, or it is deleted
 * (a deleted post reads as missing).
 */
public class PostNotFoundException extends RuntimeException {

    public PostNotFoundException(UUID postId) {
        super("Post %s not found".formatted(postId));
    }
}
