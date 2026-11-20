package ru.job4j.socialmediaapi.friendships.internal.application.model;

import java.util.List;

/**
 * Результат сценария «получить страницу заявок в друзья текущего пользователя».
 *
 * @param offers заявки страницы, сначала новые
 * @param next   курсор следующей страницы; {@code null}, если заявок больше нет
 */
public record FriendshipOfferPageResult(
        List<FriendshipOfferResult> offers,
        FriendshipOfferCursorResult next
) {
}
