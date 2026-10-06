package ru.job4j.socialmediaapi.repository;

import ru.job4j.socialmediaapi.domain.OfferFriendshipStatus;

import java.time.Instant;
import java.util.UUID;

public interface OfferFriendshipRepository {

    CreateOfferFriendshipResponse createOfferFriendship(
            CreateOfferFriendshipRequest request
    );

    record CreateOfferFriendshipRequest(
            UUID id,
            UUID fromUserId,
            UUID toUserId,
            OfferFriendshipStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    record CreateOfferFriendshipResponse(
            UUID id,
            UUID fromUserId,
            UUID toUserId,
            OfferFriendshipStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
