package ru.job4j.socialmediaapi.posts.internal.application.model;

import java.util.UUID;

/**
 * Команда сценария «опубликовать пост».
 *
 * @param actorId автор поста — текущий пользователь
 * @param title   заголовок
 * @param content текст
 */
public record CreatePostCommand(
        UUID actorId,
        String title,
        String content
) {
}
