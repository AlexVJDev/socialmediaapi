package ru.job4j.socialmediaapi.friendships.internal.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Friendship of two users created by an accepted offer. Friendship is mutual, so a pair is stored
 * once, in canonical order: {@code firstUserId < secondUserId} as PostgreSQL compares {@code uuid}
 * (the {@code CHECK} of the {@code friendships} table only backs this invariant up).
 *
 * <p>PostgreSQL compares {@code uuid} byte by byte as unsigned numbers. {@link UUID#compareTo}
 * compares two signed {@code long}s and gives another order for UUIDs with the high bit set
 * (e.g. {@code 80000000-...} is less than {@code 00000000-...-000000000001} for it). The lowercase
 * hex form of {@link UUID#toString} has a fixed length and layout, so comparing it as a string
 * gives exactly the PostgreSQL order.
 *
 * @param id                friendship id
 * @param firstUserId       first user of the pair in canonical order
 * @param secondUserId      second user of the pair in canonical order
 * @param friendshipOfferId accepted offer that created the friendship
 * @param status            status of the friendship
 * @param createdAt         creation time
 */
public record Friendship(
        UUID id,
        UUID firstUserId,
        UUID secondUserId,
        UUID friendshipOfferId,
        FriendshipStatus status,
        Instant createdAt
) {

    public Friendship {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(firstUserId, "firstUserId");
        Objects.requireNonNull(secondUserId, "secondUserId");
        Objects.requireNonNull(friendshipOfferId, "friendshipOfferId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        if (compareAsPostgres(firstUserId, secondUserId) >= 0) {
            throw new IllegalArgumentException(
                    "Users of a friendship must be different and in canonical order: %s, %s"
                            .formatted(firstUserId, secondUserId)
            );
        }
    }

    /**
     * Creates an active friendship of two users in canonical order, whatever order they are passed in.
     *
     * @param userA             one user of the pair
     * @param userB             the other user of the pair
     * @param friendshipOfferId accepted offer that creates the friendship
     * @param now               creation time
     * @return the new friendship with a generated id
     * @throws IllegalArgumentException the users are the same
     */
    public static Friendship between(UUID userA, UUID userB, UUID friendshipOfferId, Instant now) {
        Objects.requireNonNull(userA, "userA");
        Objects.requireNonNull(userB, "userB");
        var aFirst = compareAsPostgres(userA, userB) < 0;
        return new Friendship(
                UUID.randomUUID(),
                aFirst ? userA : userB,
                aFirst ? userB : userA,
                friendshipOfferId,
                FriendshipStatus.ACTIVE,
                now
        );
    }

    /**
     * Compares UUIDs in the order of PostgreSQL {@code uuid} (unsigned, byte by byte).
     *
     * @param left  first UUID
     * @param right second UUID
     * @return negative, zero or positive, like {@link Comparable#compareTo}
     */
    private static int compareAsPostgres(UUID left, UUID right) {
        return left.toString().compareTo(right.toString());
    }
}
