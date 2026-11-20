package ru.job4j.socialmediaapi.friendships.internal.application.model;

import java.util.List;

/**
 * Результат сценария «получить страницу друзей пользователя».
 *
 * @param friends друзья страницы, сначала новые дружбы
 * @param next    курсор следующей страницы; {@code null}, если друзей больше нет
 */
public record FriendPageResult(
        List<FriendResult> friends,
        FriendCursorResult next
) {
}
