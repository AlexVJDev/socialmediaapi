package ru.job4j.socialmediaapi.friendships.internal.application.model;

import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendshipOfferDirection;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Команда сценария «получить страницу заявок в друзья текущего пользователя». Без курсора
 * (оба поля {@code afterCreatedAt} и {@code afterId} — {@code null}) читается первая страница,
 * иначе — заявки строго после курсора; курсор берётся из
 * {@link FriendshipOfferPageResult#next()} предыдущей страницы.
 *
 * @param userId         текущий пользователь, чьи заявки читаются
 * @param direction      {@code INCOMING} — пользователь получатель, {@code OUTGOING} — отправитель
 * @param status         статус заявок
 * @param afterCreatedAt время создания последней показанной заявки, {@code null} для первой страницы
 * @param afterId        id последней показанной заявки, {@code null} для первой страницы
 * @param limit          максимальное число заявок на странице
 */
public record GetFriendshipOffersCommand(
        UUID userId,
        FriendshipOfferDirection direction,
        FriendshipOfferStatus status,
        Instant afterCreatedAt,
        UUID afterId,
        int limit
) {
}
