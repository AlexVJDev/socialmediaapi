package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.job4j.socialmediaapi.friendships.internal.application.exception.FriendshipAlreadyExistsException;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipsRepository;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendCursor;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendPageResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.jpa.FriendView;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.jpa.FriendshipJpaRepository;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.mapper.FriendshipPersistenceMapper;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-реализация порта {@link FriendshipsRepository} по таблице {@code friendships}:
 * запросы выполняет {@link FriendshipJpaRepository}, результаты преобразуются в модели порта.
 */
@Component
@RequiredArgsConstructor
public class FriendshipsPersistenceAdapter implements FriendshipsRepository {

    /** Запросы Spring Data JPA к {@code friendships}. */
    private final FriendshipJpaRepository jpaRepository;

    /** Преобразует сущности и строки страниц в модели порта. */
    private final FriendshipPersistenceMapper mapper;

    /**
     * Вставка пропускает дубликат пары ({@code ON CONFLICT DO NOTHING}) без ошибки PostgreSQL;
     * пропуск переводится в {@link FriendshipAlreadyExistsException}.
     */
    @Override
    public CreateFriendshipResponse createFriendship(CreateFriendshipRequest request) {
        int inserted = jpaRepository.create(
                request.id(),
                request.firstUserId(),
                request.secondUserId(),
                request.friendshipOfferId(),
                request.status().name(),
                request.createdAt()
        );
        if (inserted == 0) {
            throw new FriendshipAlreadyExistsException(request.firstUserId(), request.secondUserId(), null);
        }
        return mapper.toResponse(jpaRepository.findById(request.id()).orElseThrow());
    }

    @Override
    public FriendPageResponse findFriendsPage(UUID userId, int limit) {
        Objects.requireNonNull(userId, "userId");
        checkLimit(limit);
        return toPage(jpaRepository.findFriendsFirstPage(userId, limit + 1), limit);
    }

    @Override
    public FriendPageResponse findFriendsPageAfter(UUID userId, FriendCursor after, int limit) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(after, "after");
        checkLimit(limit);
        return toPage(
                jpaRepository.findFriendsPageAfter(userId, after.createdAt(), after.id(), limit + 1),
                limit
        );
    }

    /**
     * Собирает страницу из до {@code limit + 1} прочитанных строк: лишняя строка не возвращается,
     * она лишь означает, что есть следующая страница.
     *
     * @param rows  прочитанные строки
     * @param limit максимальное число друзей на странице
     * @return страница с курсором следующей, если она есть
     */
    private FriendPageResponse toPage(List<FriendView> rows, int limit) {
        boolean hasNext = rows.size() > limit;
        List<FriendView> pageRows = hasNext ? rows.subList(0, limit) : rows;
        Optional<FriendCursor> next = hasNext
                ? Optional.of(mapper.toCursor(pageRows.getLast()))
                : Optional.empty();
        return new FriendPageResponse(
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
