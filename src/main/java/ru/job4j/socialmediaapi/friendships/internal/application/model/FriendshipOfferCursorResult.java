package ru.job4j.socialmediaapi.friendships.internal.application.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Cursor of the next page of all offers: the position of the last offer of the page.
 *
 * @param createdAt time of creation of the last offer of the page
 * @param id        id of the last offer of the page
 */
public record FriendshipOfferCursorResult(
        Instant createdAt,
        UUID id
) {
}
