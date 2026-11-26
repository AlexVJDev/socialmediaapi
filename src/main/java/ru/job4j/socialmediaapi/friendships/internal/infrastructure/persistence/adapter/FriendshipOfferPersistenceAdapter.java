package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.job4j.socialmediaapi.friendships.internal.application.exception.FriendshipOfferAlreadyExistsException;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipOfferRepository;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FindFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendshipOfferCursor;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendshipOfferDirection;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendshipOfferPageResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.UpdateFriendshipOfferStatusRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.UpdateFriendshipOfferStatusResponse;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.jpa.FriendshipOfferEntity;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.jpa.FriendshipOfferJpaRepository;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.mapper.FriendshipOfferPersistenceMapper;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-реализация порта {@link FriendshipOfferRepository} по таблице {@code friendship_offer}:
 * запросы выполняет {@link FriendshipOfferJpaRepository}, сущности преобразуются в модели порта.
 */
@Component
@RequiredArgsConstructor
public class FriendshipOfferPersistenceAdapter implements FriendshipOfferRepository {

    /** Запросы Spring Data JPA к {@code friendship_offer}. */
    private final FriendshipOfferJpaRepository jpaRepository;

    /** Преобразует сущности в модели порта. */
    private final FriendshipOfferPersistenceMapper mapper;

    /**
     * Вставка пропускает дубликат ({@code ON CONFLICT DO NOTHING}) без ошибки PostgreSQL,
     * поэтому транзакция вызывающего остаётся рабочей и он может прочитать существующую заявку.
     */
    @Override
    public CreateFriendshipOfferResponse createFriendshipOffer(CreateFriendshipOfferRequest request) {
        int inserted = jpaRepository.create(
                request.id(),
                request.fromUserId(),
                request.toUserId(),
                request.status().name(),
                request.createdAt()
        );
        if (inserted == 0) {
            throw new FriendshipOfferAlreadyExistsException(request.fromUserId(), request.toUserId(), null);
        }
        return mapper.toResponse(jpaRepository.findById(request.id()).orElseThrow());
    }

    @Override
    public Optional<FindFriendshipOfferResponse> findByPair(UUID userA, UUID userB) {
        return jpaRepository.findByPairForUpdate(userA, userB).map(mapper::toFindResponse);
    }

    @Override
    public Optional<UpdateFriendshipOfferStatusResponse> updateStatusIfPending(
            UpdateFriendshipOfferStatusRequest request
    ) {
        int updated = jpaRepository.updateStatusIfPending(
                request.offerId(), request.actorId(), request.status(), request.updatedAt()
        );
        if (updated == 0) {
            return Optional.empty();
        }
        return jpaRepository.findById(request.offerId()).map(mapper::toUpdateResponse);
    }

    @Override
    public Optional<FindFriendshipOfferResponse> findByIdAndRecipient(UUID offerId, UUID recipientId) {
        return jpaRepository.findByIdAndToUserId(offerId, recipientId).map(mapper::toFindResponse);
    }

    @Override
    public Optional<FindFriendshipOfferResponse> findById(UUID offerId) {
        return jpaRepository.findById(offerId).map(mapper::toFindResponse);
    }

    @Override
    public FriendshipOfferPageResponse findPage(
            UUID userId,
            FriendshipOfferDirection direction,
            FriendshipOfferStatus status,
            int limit
    ) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(status, "status");
        checkLimit(limit);
        List<FriendshipOfferEntity> rows = switch (direction) {
            case INCOMING -> jpaRepository.findIncomingFirstPage(userId, status.name(), limit + 1);
            case OUTGOING -> jpaRepository.findOutgoingFirstPage(userId, status.name(), limit + 1);
        };
        return toPage(rows, limit);
    }

    @Override
    public FriendshipOfferPageResponse findPageAfter(
            UUID userId,
            FriendshipOfferDirection direction,
            FriendshipOfferStatus status,
            FriendshipOfferCursor after,
            int limit
    ) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(after, "after");
        checkLimit(limit);
        List<FriendshipOfferEntity> rows = switch (direction) {
            case INCOMING -> jpaRepository.findIncomingPageAfter(
                    userId, status.name(), after.createdAt(), after.id(), limit + 1);
            case OUTGOING -> jpaRepository.findOutgoingPageAfter(
                    userId, status.name(), after.createdAt(), after.id(), limit + 1);
        };
        return toPage(rows, limit);
    }

    /**
     * Собирает страницу из до {@code limit + 1} прочитанных строк: лишняя строка не возвращается,
     * она лишь означает, что есть следующая страница.
     *
     * @param rows  прочитанные заявки
     * @param limit максимальное число заявок на странице
     * @return страница с курсором следующей, если она есть
     */
    private FriendshipOfferPageResponse toPage(List<FriendshipOfferEntity> rows, int limit) {
        boolean hasNext = rows.size() > limit;
        List<FriendshipOfferEntity> pageRows = hasNext ? rows.subList(0, limit) : rows;
        Optional<FriendshipOfferCursor> next = hasNext
                ? Optional.of(mapper.toCursor(pageRows.getLast()))
                : Optional.empty();
        return new FriendshipOfferPageResponse(
                pageRows.stream().map(mapper::toFindResponse).toList(),
                next
        );
    }

    /**
     * Отклоняет размер страницы, который хранилище не обслуживает, до запроса.
     *
     * @param limit запрошенный размер страницы
     * @throws IllegalArgumentException {@code limit} меньше 1 или больше {@link #MAX_LIMIT}
     */
    private static void checkLimit(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException(
                    "Page limit must be from 1 to %d, got %d".formatted(MAX_LIMIT, limit));
        }
    }
}
