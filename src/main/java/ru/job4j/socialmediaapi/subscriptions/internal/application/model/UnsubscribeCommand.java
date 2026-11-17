package ru.job4j.socialmediaapi.subscriptions.internal.application.model;

import java.util.UUID;

/**
 * Команда сценария «отписаться».
 *
 * @param followerId подписчик — текущий пользователь
 * @param followedId пользователь, от которого отписываются
 */
public record UnsubscribeCommand(
        UUID followerId,
        UUID followedId
) {
}
