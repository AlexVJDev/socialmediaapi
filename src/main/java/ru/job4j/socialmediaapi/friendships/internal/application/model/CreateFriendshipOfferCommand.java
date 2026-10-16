package ru.job4j.socialmediaapi.friendships.internal.application.model;

import java.util.UUID;

/**
 * Command of the scenario «create a friendship offer».
 *
 * @param fromUserId sender of the offer
 * @param toUserId   recipient of the offer
 */
public record CreateFriendshipOfferCommand(
        UUID fromUserId,
        UUID toUserId
) {
}
