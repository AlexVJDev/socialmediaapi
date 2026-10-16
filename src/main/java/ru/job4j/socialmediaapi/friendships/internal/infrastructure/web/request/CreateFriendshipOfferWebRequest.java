package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateFriendshipOfferWebRequest(
        @NotNull UUID fromUserId,
        @NotNull UUID toUserId
) {
}
