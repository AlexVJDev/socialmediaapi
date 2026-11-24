package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.jpa;

import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Создаёт {@link FriendshipEntity} для тестов других пакетов:
 * конструктор сущности со всеми полями package-private.
 */
public final class FriendshipEntityFixtures {

    private FriendshipEntityFixtures() {
    }

    /**
     * @param id                id дружбы
     * @param firstUserId       меньший UUID участника
     * @param secondUserId      больший UUID участника
     * @param friendshipOfferId принятая заявка
     * @param status            статус
     * @param createdAt         время создания
     * @return сущность с заданными полями
     */
    public static FriendshipEntity friendship(
            UUID id, UUID firstUserId, UUID secondUserId, UUID friendshipOfferId,
            FriendshipStatus status, Instant createdAt
    ) {
        return new FriendshipEntity(id, firstUserId, secondUserId, friendshipOfferId, status, createdAt);
    }
}
