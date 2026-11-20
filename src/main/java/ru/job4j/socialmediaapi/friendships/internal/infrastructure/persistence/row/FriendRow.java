package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.row;

import java.time.Instant;
import java.util.UUID;

/**
 * Строка страницы друзей: дружба с точки зрения одного из её участников.
 *
 * @param id        id дружбы
 * @param friendId  второй участник дружбы
 * @param createdAt время создания дружбы
 */
public record FriendRow(
        UUID id,
        UUID friendId,
        Instant createdAt
) {
}
