package ru.job4j.socialmediaapi.friendships.internal.application.port.model;

import java.util.List;
import java.util.Optional;

/**
 * Page of all offers, newest first.
 *
 * @param offers offers of the page
 * @param next   position of the last offer of the page; empty if there are no more offers
 */
public record FriendshipOfferPageResponse(
        List<FindFriendshipOfferResponse> offers,
        Optional<FriendshipOfferCursor> next
) {
}
