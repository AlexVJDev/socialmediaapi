package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.UUID;

/**
 * Строка страницы друзей — дружба с точки зрения одного из её участников (проекция нативного запроса
 * {@link FriendshipJpaRepository}). Имена методов совпадают с псевдонимами колонок запроса.
 */
public interface FriendView {

    /**
     * @return id дружбы
     */
    UUID getId();

    /**
     * @return второй участник дружбы — друг пользователя, чья страница читается
     */
    UUID getFriendId();

    /**
     * @return время создания дружбы
     */
    Instant getCreatedAt();
}
