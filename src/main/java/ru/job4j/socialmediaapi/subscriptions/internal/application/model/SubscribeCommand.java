package ru.job4j.socialmediaapi.subscriptions.internal.application.model;

import java.util.UUID;

/**
 * Команда сценария «подписаться».
 *
 * @param followerId подписчик — текущий пользователь
 * @param followedId пользователь, на которого подписываются
 */
public record SubscribeCommand(
        UUID followerId,
        UUID followedId
) {
}
