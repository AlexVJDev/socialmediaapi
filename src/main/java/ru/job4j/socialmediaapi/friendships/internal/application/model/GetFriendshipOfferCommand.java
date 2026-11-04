package ru.job4j.socialmediaapi.friendships.internal.application.model;

import java.util.UUID;

/**
 * Command of the scenario «get a friendship offer».
 *
 * @param offerId offer to read
 */
public record GetFriendshipOfferCommand(
        UUID offerId
) {
}
