package ru.job4j.socialmediaapi.friendships.internal.application.port.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Позиция дружбы в порядке страницы друзей ({@code created_at DESC, id DESC}):
 * следующая страница начинается строго после неё.
 *
 * @param createdAt время создания дружбы
 * @param id        id дружбы, различает дружбы с одинаковым {@code createdAt}
 */
public record FriendCursor(
        Instant createdAt,
        UUID id
) {
}
