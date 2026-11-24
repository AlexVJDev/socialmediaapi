package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Запросы Spring Data JPA к таблице {@code friendships}.
 *
 * <p>Наследует {@link Repository}, а не {@code JpaRepository}: доступны только объявленные методы,
 * запросов по всей таблице нет. Каждый запрос ограничен {@code id} или участником дружбы.
 *
 * <p>Транзакции — как у {@code SimpleJpaRepository}: чтения {@code readOnly}, запись — в транзакции записи.
 * Метод присоединяется к транзакции вызывающего сценария, а без неё открывает свою.
 */
@Transactional(readOnly = true)
public interface FriendshipJpaRepository extends Repository<FriendshipEntity, UUID> {

    /**
     * Атомарно вставляет дружбу. Если дружба этой пары уже есть ({@code uq_friendships_users}), строка
     * не вставляется и ошибки нет — транзакция остаётся рабочей. Нарушения CHECK и внешнего ключа
     * по-прежнему приводят к ошибке.
     *
     * @param id                id новой дружбы
     * @param firstUserId       меньший UUID участника
     * @param secondUserId      больший UUID участника
     * @param friendshipOfferId принятая заявка
     * @param status            статус дружбы (имя enum)
     * @param createdAt         время создания
     * @return 1 — дружба вставлена, 0 — у пары уже есть дружба
     */
    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO friendships (
                id, first_user_id, second_user_id,
                friendship_offer_id, status, created_at
            )
            VALUES (:id, :firstUserId, :secondUserId, :friendshipOfferId, :status, :createdAt)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int create(
            @Param("id") UUID id,
            @Param("firstUserId") UUID firstUserId,
            @Param("secondUserId") UUID secondUserId,
            @Param("friendshipOfferId") UUID friendshipOfferId,
            @Param("status") String status,
            @Param("createdAt") Instant createdAt
    );

    /**
     * @param id id дружбы
     * @return дружба; пусто, если её нет
     */
    Optional<FriendshipEntity> findById(UUID id);

    /**
     * Первая страница активных друзей пользователя, сначала новые ({@code created_at DESC, id DESC}).
     * Пользователь может быть любой стороной канонической пары, поэтому страница — слияние двух веток;
     * каждая читается по своему индексу ({@code idx_friendships_first_user_page},
     * {@code idx_friendships_second_user_page}) и обрезается до размера страницы до слияния.
     *
     * @param userId пользователь, чьи друзья читаются
     * @param limit  число читаемых строк
     * @return друзья пользователя
     */
    @Query(value = """
            (SELECT id, second_user_id AS friend_id, created_at
             FROM friendships
             WHERE first_user_id = :userId AND status = 'ACTIVE'
             ORDER BY created_at DESC, id DESC
             LIMIT :limit)
            UNION ALL
            (SELECT id, first_user_id AS friend_id, created_at
             FROM friendships
             WHERE second_user_id = :userId AND status = 'ACTIVE'
             ORDER BY created_at DESC, id DESC
             LIMIT :limit)
            ORDER BY created_at DESC, id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<FriendView> findFriendsFirstPage(
            @Param("userId") UUID userId,
            @Param("limit") int limit
    );

    /**
     * Страница активных друзей пользователя строго после курсора, в том же порядке.
     *
     * @param userId         пользователь, чьи друзья читаются
     * @param afterCreatedAt {@code created_at} последней показанной дружбы
     * @param afterId        {@code id} последней показанной дружбы
     * @param limit          число читаемых строк
     * @return друзья пользователя после курсора
     */
    @Query(value = """
            (SELECT id, second_user_id AS friend_id, created_at
             FROM friendships
             WHERE first_user_id = :userId AND status = 'ACTIVE'
               AND (created_at, id) < (:afterCreatedAt, :afterId)
             ORDER BY created_at DESC, id DESC
             LIMIT :limit)
            UNION ALL
            (SELECT id, first_user_id AS friend_id, created_at
             FROM friendships
             WHERE second_user_id = :userId AND status = 'ACTIVE'
               AND (created_at, id) < (:afterCreatedAt, :afterId)
             ORDER BY created_at DESC, id DESC
             LIMIT :limit)
            ORDER BY created_at DESC, id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<FriendView> findFriendsPageAfter(
            @Param("userId") UUID userId,
            @Param("afterCreatedAt") Instant afterCreatedAt,
            @Param("afterId") UUID afterId,
            @Param("limit") int limit
    );
}
