package ru.job4j.socialmediaapi.friendships.internal.application.port.model;

import java.util.UUID;

/**
 * Друг пользователя, прочитанный из таблицы {@code friendships}.
 *
 * @param id     id дружбы
 * @param userId второй участник дружбы
 */
public record FindFriendResponse(
        UUID id,
        UUID userId
) {
}
