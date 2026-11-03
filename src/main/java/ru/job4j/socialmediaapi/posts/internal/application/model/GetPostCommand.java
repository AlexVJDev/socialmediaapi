package ru.job4j.socialmediaapi.posts.internal.application.model;

import java.util.UUID;

/**
 * Command of the scenario «get a post».
 *
 * @param postId post to read
 */
public record GetPostCommand(
        UUID postId
) {
}
