package ru.job4j.socialmediaapi.friendships.internal.application.port.model;

import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Friendship to store; users are in canonical order
 * ({@code firstUserId < secondUserId} as PostgreSQL compares UUIDs).
 *
 * @param id                id
 * @param firstUserId       first user of the pair in canonical order
 * @param secondUserId      second user of the pair in canonical order
 * @param friendshipOfferId accepted offer that created the friendship
 * @param status            status
 * @param createdAt         time of creation
 */
public record CreateFriendshipRequest(
        UUID id,
        UUID firstUserId,
        UUID secondUserId,
        UUID friendshipOfferId,
        FriendshipStatus status,
        Instant createdAt
) {
}
