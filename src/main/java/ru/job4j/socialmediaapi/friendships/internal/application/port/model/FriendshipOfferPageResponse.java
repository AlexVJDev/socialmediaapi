package ru.job4j.socialmediaapi.friendships.internal.application.port.model;

import java.util.List;
import java.util.Optional;

/**
 * Страница заявок пользователя, сначала новые.
 *
 * @param offers заявки страницы
 * @param next   позиция последней заявки страницы; пусто, если заявок больше нет
 */
public record FriendshipOfferPageResponse(
        List<FindFriendshipOfferResponse> offers,
        Optional<FriendshipOfferCursor> next
) {
}
