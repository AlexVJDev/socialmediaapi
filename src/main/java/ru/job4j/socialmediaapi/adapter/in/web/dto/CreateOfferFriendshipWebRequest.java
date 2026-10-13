package ru.job4j.socialmediaapi.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateOfferFriendshipWebRequest(
        @NotNull UUID fromUserId,
        @NotNull UUID toUserId
) {
}
