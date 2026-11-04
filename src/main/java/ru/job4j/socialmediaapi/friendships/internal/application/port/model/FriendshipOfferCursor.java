package ru.job4j.socialmediaapi.friendships.internal.application.port.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Position of an offer in the order of all offers ({@code created_at DESC, id DESC}):
 * the next page starts strictly after it.
 *
 * @param createdAt time of creation of the offer
 * @param id        offer id, breaks ties of {@code createdAt}
 */
public record FriendshipOfferCursor(
        Instant createdAt,
        UUID id
) {
}
