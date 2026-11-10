package ru.job4j.socialmediaapi.subscriptions.internal.application.model;

import java.util.UUID;

/**
 * Command of the scenario «get the followers of a user».
 *
 * @param userId user whose followers are read
 */
public record GetFollowersCommand(
        UUID userId
) {
}
