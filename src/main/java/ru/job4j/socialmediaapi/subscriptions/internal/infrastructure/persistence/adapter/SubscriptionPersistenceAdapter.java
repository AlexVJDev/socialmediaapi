package ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.job4j.socialmediaapi.subscriptions.internal.application.exception.SubscriptionAlreadyExistsException;
import ru.job4j.socialmediaapi.subscriptions.internal.application.port.SubscriptionRepository;
import ru.job4j.socialmediaapi.subscriptions.internal.application.port.model.CreateSubscriptionRequest;
import ru.job4j.socialmediaapi.subscriptions.internal.application.port.model.SubscriptionResponse;
import ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.persistence.jpa.SubscriptionJpaRepository;
import ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.persistence.mapper.SubscriptionPersistenceMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-реализация порта {@link SubscriptionRepository} по таблице {@code subscriptions}:
 * запросы выполняет {@link SubscriptionJpaRepository}, сущности преобразуются в модели порта.
 */
@Component
@RequiredArgsConstructor
public class SubscriptionPersistenceAdapter implements SubscriptionRepository {

    /** Запросы Spring Data JPA к {@code subscriptions}. */
    private final SubscriptionJpaRepository jpaRepository;

    /** Преобразует сущности в модели порта. */
    private final SubscriptionPersistenceMapper mapper;

    /**
     * Вставка пропускает существующую подписку ({@code ON CONFLICT DO NOTHING}) без ошибки PostgreSQL;
     * пропуск переводится в {@link SubscriptionAlreadyExistsException}.
     */
    @Override
    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request) {
        return insert(request).orElseThrow(
                () -> new SubscriptionAlreadyExistsException(request.followerId(), request.followedId(), null)
        );
    }

    @Override
    public Optional<SubscriptionResponse> createSubscriptionIfAbsent(CreateSubscriptionRequest request) {
        return insert(request);
    }

    @Override
    public boolean existsSubscription(UUID followerId, UUID followedId) {
        return jpaRepository.existsByFollowerIdAndFollowedId(followerId, followedId);
    }

    @Override
    public Optional<SubscriptionResponse> findSubscription(UUID followerId, UUID followedId) {
        return jpaRepository.findByFollowerIdAndFollowedId(followerId, followedId).map(mapper::toResponse);
    }

    @Override
    public boolean deleteSubscription(UUID followerId, UUID followedId) {
        return jpaRepository.deleteSubscription(followerId, followedId) == 1;
    }

    @Override
    public List<SubscriptionResponse> findByFollower(UUID followerId) {
        return mapper.toResponses(jpaRepository.findByFollowerIdOrderByCreatedAtDescIdDesc(followerId));
    }

    @Override
    public List<SubscriptionResponse> findByFollowed(UUID followedId) {
        return mapper.toResponses(jpaRepository.findByFollowedIdOrderByCreatedAtDescIdDesc(followedId));
    }

    /**
     * Вставляет подписку, если её ещё нет.
     *
     * @param request подписка для сохранения
     * @return сохранённая подписка; пусто, если подписка в этом направлении уже была
     */
    private Optional<SubscriptionResponse> insert(CreateSubscriptionRequest request) {
        int inserted = jpaRepository.create(
                request.id(), request.followerId(), request.followedId(), request.createdAt()
        );
        if (inserted == 0) {
            return Optional.empty();
        }
        return jpaRepository.findById(request.id()).map(mapper::toResponse);
    }
}
