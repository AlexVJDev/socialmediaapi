package ru.job4j.socialmediaapi.friendships.internal.application.port.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Позиция заявки в порядке страницы заявок ({@code created_at DESC, id DESC}):
 * следующая страница начинается строго после неё.
 *
 * @param createdAt время создания заявки
 * @param id        id заявки, различает заявки с одинаковым {@code createdAt}
 */
public record FriendshipOfferCursor(
        Instant createdAt,
        UUID id
) {
}
