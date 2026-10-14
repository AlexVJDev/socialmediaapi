package ru.job4j.socialmediaapi.application.port.out;

import ru.job4j.socialmediaapi.domain.friendship.FriendshipStatus;
import ru.job4j.socialmediaapi.domain.friendship.exception.FriendshipAlreadyExistsException;
import ru.job4j.socialmediaapi.domain.friendship.exception.FriendshipInvalidUserOrderException;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipRequestNotFoundException;

import java.time.Instant;
import java.util.UUID;

public interface FriendshipsRepository {

    /**
     * Saves the friendship of two users stored in canonical order.
     *
     * @param request friendship to save
     * @return the saved friendship
     * @throws FriendshipAlreadyExistsException    friendship of these users already exists
     * @throws FriendshipInvalidUserOrderException {@code firstUserId} is not less than {@code secondUserId}
     * @throws FriendshipRequestNotFoundException  the referenced offer friendship does not exist
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
