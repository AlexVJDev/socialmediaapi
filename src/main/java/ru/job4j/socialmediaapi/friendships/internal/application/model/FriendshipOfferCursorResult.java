package ru.job4j.socialmediaapi.friendships.internal.application.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Курсор следующей страницы заявок: позиция последней заявки страницы.
 *
 * @param createdAt время создания последней заявки страницы
 * @param id        id последней заявки страницы
 */
public record FriendshipOfferCursorResult(
        Instant createdAt,
        UUID id
) {
}
