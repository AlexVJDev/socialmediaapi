package ru.job4j.socialmediaapi.posts.internal.application.model;

import java.util.UUID;

/**
 * Команда сценария «удалить пост».
 *
 * @param postId  пост
 * @param actorId текущий пользователь; должен быть автором поста
 */
public record DeletePostCommand(
        UUID postId,
        UUID actorId
) {
}
