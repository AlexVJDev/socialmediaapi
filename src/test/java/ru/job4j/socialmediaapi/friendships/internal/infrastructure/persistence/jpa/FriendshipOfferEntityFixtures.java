package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.jpa;

import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Создаёт {@link FriendshipOfferEntity} для тестов других пакетов:
 * конструктор сущности со всеми полями package-private.
 */
public final class FriendshipOfferEntityFixtures {

    private FriendshipOfferEntityFixtures() {
    }

    /**
     * @param id         id заявки
     * @param fromUserId отправитель
     * @param toUserId   получатель
     * @param status     статус
     * @param createdAt  время создания
     * @param updatedAt  время изменения
     * @return сущность с заданными полями
     */
    public static FriendshipOfferEntity offer(
            UUID id, UUID fromUserId, UUID toUserId,
            FriendshipOfferStatus status, Instant createdAt, Instant updatedAt
    ) {
        return new FriendshipOfferEntity(id, fromUserId, toUserId, status, createdAt, updatedAt);
    }
}
