package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.jpa;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Запросы Spring Data JPA к таблице {@code friendship_offer}.
 *
 * <p>Наследует {@link Repository}, а не {@code JpaRepository}: доступны только объявленные методы,
 * {@code findAll()}/{@code deleteAll()} (запросы по всей таблице) нет. Каждый запрос ограничен
 * {@code id}, получателем или отправителем заявки.
 *
 * <p>Транзакции — как у {@code SimpleJpaRepository}: чтения {@code readOnly}, запись и блокировка — в транзакции
 * записи. Метод присоединяется к транзакции вызывающего сценария, а без неё открывает свою: JPA не выполняет
 * {@code @Modifying}-запросы и блокировки вне транзакции.
 */
@Transactional(readOnly = true)
public interface FriendshipOfferJpaRepository extends Repository<FriendshipOfferEntity, UUID> {

    /**
     * Атомарно вставляет заявку. Если у пары уже есть заявка в любом направлении (уникальный индекс
     * {@code uq_friendship_offer_pair}), строка не вставляется и ошибки нет — транзакция остаётся рабочей.
     * Цель {@code ON CONFLICT} не указана: уникальность задана индексом по выражению, а не по колонкам.
     *
     * @param id         id новой заявки
     * @param fromUserId отправитель
     * @param toUserId   получатель
     * @param status     статус новой заявки (имя enum)
     * @param now        время создания и изменения
     * @return 1 — заявка вставлена, 0 — у пары уже есть заявка
     */
    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO friendship_offer (
                id, from_user_id, to_user_id,
                status, created_at, updated_at
            )
            VALUES (:id, :fromUserId, :toUserId, :status, :now, :now)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int create(
            @Param("id") UUID id,
            @Param("fromUserId") UUID fromUserId,
            @Param("toUserId") UUID toUserId,
            @Param("status") String status,
            @Param("now") Instant now
    );

    /**
     * @param id id заявки
     * @return заявка в любом статусе; пусто, если её нет
     */
    Optional<FriendshipOfferEntity> findById(UUID id);

    /**
     * @param id       id заявки
     * @param toUserId ожидаемый получатель
     * @return заявка; пусто, если её нет или получатель другой
     */
    Optional<FriendshipOfferEntity> findByIdAndToUserId(UUID id, UUID toUserId);

    /**
     * Находит заявку пары в любом направлении и блокирует строку до конца транзакции.
     *
     * @param userA один пользователь пары
     * @param userB другой пользователь пары
     * @return заявка пары; пусто, если её нет
     */
    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o FROM FriendshipOfferEntity o
            WHERE (o.fromUserId = :userA AND o.toUserId = :userB)
               OR (o.fromUserId = :userB AND o.toUserId = :userA)
            """)
    Optional<FriendshipOfferEntity> findByPairForUpdate(
            @Param("userA") UUID userA,
            @Param("userB") UUID userB
    );

    /**
     * Меняет статус {@code PENDING}-заявки данного получателя одним запросом. Одновременное решение
     * ждёт блокировку строки, взятую первым, и после его коммита условие {@code status = PENDING} уже ложно.
     * {@code clearAutomatically}: следующее чтение берёт строку из БД, а не устаревшую копию из контекста.
     *
     * @param id        id заявки
     * @param toUserId  получатель, который решает
     * @param status    новый статус
     * @param updatedAt время решения
     * @return 1 — статус изменён; 0 — заявки нет, получатель другой или заявка уже не {@code PENDING}
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE FriendshipOfferEntity o
            SET o.status = :status, o.updatedAt = :updatedAt
            WHERE o.id = :id AND o.toUserId = :toUserId
              AND o.status = ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus.PENDING
            """)
    int updateStatusIfPending(
            @Param("id") UUID id,
            @Param("toUserId") UUID toUserId,
            @Param("status") FriendshipOfferStatus status,
            @Param("updatedAt") Instant updatedAt
    );

    /**
     * Первая страница входящих заявок; фильтр и порядок совпадают с индексом {@code idx_friendship_offer_incoming}.
     *
     * @param toUserId получатель
     * @param status   статус заявок (имя enum)
     * @param limit    число читаемых строк
     * @return заявки, сначала новые
     */
    @Query(value = """
            SELECT id, from_user_id, to_user_id,
                   status, created_at, updated_at
            FROM friendship_offer
            WHERE to_user_id = :toUserId AND status = :status
            ORDER BY created_at DESC, id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<FriendshipOfferEntity> findIncomingFirstPage(
            @Param("toUserId") UUID toUserId,
            @Param("status") String status,
            @Param("limit") int limit
    );

    /**
     * Страница входящих заявок после курсора; сравнение строк {@code (created_at, id) < (…)} — условие индекса.
     *
     * @param toUserId       получатель
     * @param status         статус заявок (имя enum)
     * @param afterCreatedAt {@code created_at} последней показанной заявки
     * @param afterId        {@code id} последней показанной заявки
     * @param limit          число читаемых строк
     * @return заявки после курсора, сначала новые
     */
    @Query(value = """
            SELECT id, from_user_id, to_user_id,
                   status, created_at, updated_at
            FROM friendship_offer
            WHERE to_user_id = :toUserId AND status = :status
              AND (created_at, id) < (:afterCreatedAt, :afterId)
            ORDER BY created_at DESC, id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<FriendshipOfferEntity> findIncomingPageAfter(
            @Param("toUserId") UUID toUserId,
            @Param("status") String status,
            @Param("afterCreatedAt") Instant afterCreatedAt,
            @Param("afterId") UUID afterId,
            @Param("limit") int limit
    );

    /**
     * Первая страница исходящих заявок; фильтр и порядок совпадают с индексом {@code idx_friendship_offer_outgoing}.
     *
     * @param fromUserId отправитель
     * @param status     статус заявок (имя enum)
     * @param limit      число читаемых строк
     * @return заявки, сначала новые
     */
    @Query(value = """
            SELECT id, from_user_id, to_user_id,
                   status, created_at, updated_at
            FROM friendship_offer
            WHERE from_user_id = :fromUserId AND status = :status
            ORDER BY created_at DESC, id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<FriendshipOfferEntity> findOutgoingFirstPage(
            @Param("fromUserId") UUID fromUserId,
            @Param("status") String status,
            @Param("limit") int limit
    );

    /**
     * Страница исходящих заявок после курсора.
     *
     * @param fromUserId     отправитель
     * @param status         статус заявок (имя enum)
     * @param afterCreatedAt {@code created_at} последней показанной заявки
     * @param afterId        {@code id} последней показанной заявки
     * @param limit          число читаемых строк
     * @return заявки после курсора, сначала новые
     */
    @Query(value = """
            SELECT id, from_user_id, to_user_id,
                   status, created_at, updated_at
            FROM friendship_offer
            WHERE from_user_id = :fromUserId AND status = :status
              AND (created_at, id) < (:afterCreatedAt, :afterId)
            ORDER BY created_at DESC, id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<FriendshipOfferEntity> findOutgoingPageAfter(
            @Param("fromUserId") UUID fromUserId,
            @Param("status") String status,
            @Param("afterCreatedAt") Instant afterCreatedAt,
            @Param("afterId") UUID afterId,
            @Param("limit") int limit
    );
}
