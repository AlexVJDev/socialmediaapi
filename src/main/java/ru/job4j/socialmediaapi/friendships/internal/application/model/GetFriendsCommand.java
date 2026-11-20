package ru.job4j.socialmediaapi.friendships.internal.application.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Команда сценария «получить страницу друзей пользователя». Без курсора
 * (оба поля {@code afterCreatedAt} и {@code afterId} — {@code null}) читается первая страница,
 * иначе — друзья строго после курсора; курсор берётся из
 * {@link FriendPageResult#next()} предыдущей страницы.
 *
 * @param userId         пользователь, чьи друзья читаются
 * @param afterCreatedAt время создания последней показанной дружбы, {@code null} для первой страницы
 * @param afterId        id последней показанной дружбы, {@code null} для первой страницы
 * @param limit          максимальное число друзей на странице
 */
public record GetFriendsCommand(
        UUID userId,
        Instant afterCreatedAt,
        UUID afterId,
        int limit
) {
}
