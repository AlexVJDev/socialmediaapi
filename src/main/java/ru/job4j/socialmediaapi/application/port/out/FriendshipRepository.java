package ru.job4j.socialmediaapi.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface FriendshipRepository {

    CreateFriendshipResponse createFriendship(
            CreateFriendshipRequest request
    );

    record CreateFriendshipRequest(
            UUID id,
            UUID firstUserId,
            UUID secondUserId,
            UUID offerFriendshipId,
            Instant createdAt
    ) {
    }

    record CreateFriendshipResponse(
            UUID id,
            UUID firstUserId,
            UUID secondUserId,
            UUID offerFriendshipId,
            Instant createdAt
    ) {
    }
}
