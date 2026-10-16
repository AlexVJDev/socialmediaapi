package ru.job4j.socialmediaapi.friendships.internal.application.port.model;

import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Friendship as stored after creation.
 *
 * @param id                id
 * @param firstUserId       first user of the pair in canonical order
 * @param secondUserId      second user of the pair in canonical order
 * @param friendshipOfferId accepted offer that created the friendship
 * @param status            status
 * @param createdAt         time of creation
 */
public record CreateFriendshipResponse(
        UUID id,
        UUID firstUserId,
        UUID secondUserId,
        UUID friendshipOfferId,
        FriendshipStatus status,
        Instant createdAt
) {
}
