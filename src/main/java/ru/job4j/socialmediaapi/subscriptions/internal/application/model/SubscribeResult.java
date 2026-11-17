package ru.job4j.socialmediaapi.subscriptions.internal.application.model;

/**
 * Результат сценария «подписаться».
 *
 * @param subscription созданная или уже существовавшая подписка
 * @param created      {@code true} — подписка создана сейчас, {@code false} — уже была
 */
public record SubscribeResult(
        SubscriptionResult subscription,
        boolean created
) {
}
