package ru.job4j.socialmediaapi.friendships.internal.application.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Курсор следующей страницы друзей: позиция последней дружбы страницы.
 *
 * @param createdAt время создания последней дружбы страницы
 * @param id        id последней дружбы страницы
 */
public record FriendCursorResult(
        Instant createdAt,
        UUID id
) {
}
