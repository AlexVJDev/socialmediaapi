package ru.job4j.socialmediaapi.subscriptions.internal.application.model;

import java.util.UUID;

/**
 * Command of the scenario «get the subscriptions of a user».
 *
 * @param userId user whose subscriptions are read
 */
public record GetSubscriptionsCommand(
        UUID userId
) {
}
