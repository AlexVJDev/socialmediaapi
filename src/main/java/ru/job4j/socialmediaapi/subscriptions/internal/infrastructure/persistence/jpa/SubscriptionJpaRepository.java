package ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.persistence.jpa;

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
 * Запросы Spring Data JPA к таблице {@code subscriptions}.
 *
 * <p>Наследует {@link Repository}, а не {@code JpaRepository}: доступны только объявленные методы,
 * запросов по всей таблице нет. Каждый запрос ограничен {@code id}, подписчиком или тем, на кого подписались.
 *
 * <p>Транзакции — как у {@code SimpleJpaRepository}: чтения {@code readOnly}, запись — в транзакции записи.
 * Метод присоединяется к транзакции вызывающего сценария, а без неё открывает свою.
 */
@Transactional(readOnly = true)
public interface SubscriptionJpaRepository extends Repository<SubscriptionEntity, UUID> {

    /**
     * Атомарно вставляет подписку. Если подписка этой пары в этом направлении уже есть
     * ({@code uq_subscriptions_users}), строка не вставляется и ошибки нет — транзакция остаётся рабочей.
     *
     * @param id         id новой подписки
     * @param followerId подписчик
     * @param followedId пользователь, на которого подписываются
     * @param createdAt  время создания
     * @return 1 — подписка вставлена, 0 — она уже есть
     */
    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO subscriptions (
                id, follower_id, followed_id, created_at
            )
            VALUES (:id, :followerId, :followedId, :createdAt)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int create(
            @Param("id") UUID id,
            @Param("followerId") UUID followerId,
            @Param("followedId") UUID followedId,
            @Param("createdAt") Instant createdAt
    );

    /**
     * @param id id подписки
     * @return подписка; пусто, если её нет
     */
    Optional<SubscriptionEntity> findById(UUID id);

    /**
     * @param followerId подписчик
     * @param followedId пользователь, на которого подписались
     * @return {@code true}, если подписка в этом направлении есть
     */
    boolean existsByFollowerIdAndFollowedId(UUID followerId, UUID followedId);

    /**
     * @param followerId подписчик
     * @param followedId пользователь, на которого подписались
     * @return подписка в этом направлении; пусто, если её нет
     */
    Optional<SubscriptionEntity> findByFollowerIdAndFollowedId(UUID followerId, UUID followedId);

    /**
     * Удаляет подписку в одном направлении.
     *
     * @param followerId подписчик
     * @param followedId пользователь, на которого подписались
     * @return число удалённых строк: 1 или 0
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM SubscriptionEntity s
            WHERE s.followerId = :followerId AND s.followedId = :followedId
            """)
    int deleteSubscription(
            @Param("followerId") UUID followerId,
            @Param("followedId") UUID followedId
    );

    /**
     * Подписки пользователя (он — {@code follower_id}), сначала новые; индекс {@code idx_subscriptions_follower}.
     *
     * @param followerId подписчик
     * @return его подписки
     */
    List<SubscriptionEntity> findByFollowerIdOrderByCreatedAtDescIdDesc(UUID followerId);

    /**
     * Подписчики пользователя (он — {@code followed_id}), сначала новые; индекс {@code idx_subscriptions_followed}.
     *
     * @param followedId пользователь, на которого подписались
     * @return его подписчики
     */
    List<SubscriptionEntity> findByFollowedIdOrderByCreatedAtDescIdDesc(UUID followedId);
}
