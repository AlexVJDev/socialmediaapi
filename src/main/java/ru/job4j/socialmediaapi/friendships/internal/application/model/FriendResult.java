package ru.job4j.socialmediaapi.friendships.internal.application.model;

import java.util.UUID;

/**
 * Друг пользователя: элемент результата сценария «получить страницу друзей пользователя».
 *
 * @param id     id дружбы
 * @param userId второй участник дружбы
 */
public record FriendResult(
        UUID id,
        UUID userId
) {
}
