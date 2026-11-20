package ru.job4j.socialmediaapi.friendships.internal.application.port.model;

import java.util.List;
import java.util.Optional;

/**
 * Страница друзей пользователя, сначала новые дружбы.
 *
 * @param friends друзья страницы
 * @param next    позиция последней дружбы страницы; пусто, если друзей больше нет
 */
public record FriendPageResponse(
        List<FindFriendResponse> friends,
        Optional<FriendCursor> next
) {
}
