package ru.job4j.socialmediaapi.posts.internal.application.model;

import java.util.UUID;

/**
 * Команда сценария «изменить пост».
 *
 * @param postId  пост
 * @param actorId текущий пользователь; должен быть автором поста
 * @param title   новый заголовок
 * @param content новый текст
 */
public record UpdatePostCommand(
        UUID postId,
        UUID actorId,
        String title,
        String content
) {
}
