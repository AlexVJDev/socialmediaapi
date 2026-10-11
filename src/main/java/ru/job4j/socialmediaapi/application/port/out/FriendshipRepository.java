package ru.job4j.socialmediaapi.application.port.out;

import ru.job4j.socialmediaapi.domain.friendship.FriendshipStatus;
import ru.job4j.socialmediaapi.domain.friendship.exception.FriendshipAlreadyExistsException;
import ru.job4j.socialmediaapi.domain.friendship.exception.FriendshipInvalidUserOrderException;
import ru.job4j.socialmediaapi.domain.offerfriendship.exception.FriendshipRequestNotFoundException;

import java.time.Instant;
import java.util.UUID;

public interface FriendshipRepository {

    /**
     * Saves the friendship of two users stored in canonical order.
     *
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
            UUID offerFriendshipId,
            FriendshipStatus status,
            Instant createdAt
    ) {
    }

    record CreateFriendshipResponse(
            UUID id,
            UUID firstUserId,
            UUID secondUserId,
            UUID offerFriendshipId,
            FriendshipStatus status,
            Instant createdAt
    ) {
    }
}
