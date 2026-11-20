package ru.job4j.socialmediaapi.friendships.internal.application.model;

import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Заявка в друзья, как её читают сценарии «получить заявку в друзья» и
 * «получить страницу заявок в друзья текущего пользователя».
 *
 * @param id         id заявки
 * @param fromUserId отправитель заявки
 * @param toUserId   получатель заявки
 * @param status     статус
 * @param createdAt  время создания
 * @param updatedAt  время последнего изменения (для решённой заявки — время решения)
 */
public record FriendshipOfferResult(
        UUID id,
        UUID fromUserId,
        UUID toUserId,
        FriendshipOfferStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
