package ru.job4j.socialmediaapi.friendships.internal.application.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Command of the scenario «get a page of all friendship offers». Without a cursor
 * (both {@code afterCreatedAt} and {@code afterId} are {@code null}) the first page is read,
 * otherwise the offers strictly after the cursor; the cursor is taken from
 * {@link FriendshipOfferPageResult#next()} of the previous page.
 *
 * @param afterCreatedAt creation time of the last shown offer, {@code null} for the first page
 * @param afterId        id of the last shown offer, {@code null} for the first page
 * @param limit          maximum number of offers on the page
 */
public record GetFriendshipOffersCommand(
        Instant afterCreatedAt,
        UUID afterId,
        int limit
) {
}
