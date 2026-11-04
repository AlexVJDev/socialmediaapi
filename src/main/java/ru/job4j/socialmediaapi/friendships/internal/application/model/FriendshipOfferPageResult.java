package ru.job4j.socialmediaapi.friendships.internal.application.model;

import java.util.List;

/**
 * Result of the scenario «get a page of all friendship offers».
 *
 * @param offers offers of the page, newest first
 * @param next   cursor of the next page; {@code null} if there are no more offers
 */
public record FriendshipOfferPageResult(
        List<FriendshipOfferResult> offers,
        FriendshipOfferCursorResult next
) {
}
