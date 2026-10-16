package ru.job4j.socialmediaapi.friendships.internal.application.model;

import java.util.UUID;

/**
 * Command of the scenario «accept a friendship offer».
 *
 * @param offerId offer to accept
 * @param actorId user who accepts the offer; must be its recipient
 */
public record AcceptFriendshipOfferCommand(
        UUID offerId,
        UUID actorId
) {
}
