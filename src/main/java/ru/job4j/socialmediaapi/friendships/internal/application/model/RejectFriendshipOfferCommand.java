package ru.job4j.socialmediaapi.friendships.internal.application.model;

import java.util.UUID;

/**
 * Command of the scenario «reject a friendship offer».
 *
 * @param offerId offer to reject
 * @param actorId user who rejects the offer; must be its recipient
 */
public record RejectFriendshipOfferCommand(
        UUID offerId,
        UUID actorId
) {
}
