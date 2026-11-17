package ru.job4j.socialmediaapi.posts.internal.application.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Команда сценария «получить страницу постов пользователя». Без курсора
 * ({@code afterId == null}) читается первая страница, иначе — посты строго после курсора;
 * курсор берётся из {@link FeedPageResult#next()} предыдущей страницы.
 *
 * @param authorId       автор постов
 * @param afterCreatedAt время создания последнего показанного поста, {@code null} для первой страницы
 * @param afterId        id последнего показанного поста, {@code null} для первой страницы
 * @param limit          максимальное число постов на странице
 */
public record GetUserPostsCommand(
        UUID authorId,
        Instant afterCreatedAt,
        UUID afterId,
        int limit
) {
}
