package ru.job4j.socialmediaapi.friendships.internal.application.port;

import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipStatus;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipAlreadyExistsException;

import java.time.Instant;
import java.util.UUID;

public interface FriendshipsRepository {

    /**
     * Saves the friendship of two users stored in canonical order.
     *
     * @param request friendship to save
     * @return the saved friendship
     * @throws FriendshipAlreadyExistsException friendship of these users already exists
     */
    CreateFriendshipResponse createFriendship(
            CreateFriendshipRequest request
    );

    record CreateFriendshipRequest(
            UUID id,
            UUID firstUserId,
            UUID secondUserId,
            UUID friendshipOfferId,
            FriendshipStatus status,
            Instant createdAt
    ) {
    }

    record CreateFriendshipResponse(
            UUID id,
            UUID firstUserId,
            UUID secondUserId,
            UUID friendshipOfferId,
            FriendshipStatus status,
            Instant createdAt
    ) {
    }
}
