package ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.job4j.socialmediaapi.posts.internal.application.port.FeedRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedCursor;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedPageResponse;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.jpa.FeedJpaRepository;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.jpa.PostEntity;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.mapper.PostPersistenceMapper;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-реализация порта {@link FeedRepository}: запрос ленты выполняет {@link FeedJpaRepository}
 * (план запроса — в его Javadoc), посты преобразуются в модели порта. Страницы читаются по курсору
 * {@code (created_at, id)} последнего показанного поста; лишняя {@code limit + 1}-я строка только
 * сообщает, что есть следующая страница. См. {@code docs/process/Feed.md}.
 */
@Component
@RequiredArgsConstructor
public class FeedPersistenceAdapter implements FeedRepository {

    /** Запросы ленты Spring Data JPA. */
    private final FeedJpaRepository jpaRepository;

    /** Преобразует посты в модели порта и курсор. */
    private final PostPersistenceMapper mapper;

    @Override
    public FeedPageResponse findFeed(UUID followerId, int limit) {
        checkLimit(limit);
        return toPage(jpaRepository.findFeedFirstPage(followerId, limit + 1), limit);
    }

    @Override
    public FeedPageResponse findFeedAfter(UUID followerId, FeedCursor after, int limit) {
        Objects.requireNonNull(after, "after");
        checkLimit(limit);
        return toPage(
                jpaRepository.findFeedPageAfter(followerId, after.createdAt(), after.id(), limit + 1),
                limit
        );
    }

    /**
     * Собирает страницу из до {@code limit + 1} прочитанных постов: лишний пост не возвращается,
     * он только означает, что есть следующая страница.
     *
     * @param rows  прочитанные посты
     * @param limit максимальное число постов на странице
     * @return страница с курсором следующей страницы, если она есть
     */
    private FeedPageResponse toPage(List<PostEntity> rows, int limit) {
        boolean hasNext = rows.size() > limit;
        List<PostEntity> pageRows = hasNext ? rows.subList(0, limit) : rows;
        Optional<FeedCursor> next = hasNext
                ? Optional.of(mapper.toCursor(pageRows.getLast()))
                : Optional.empty();
        return new FeedPageResponse(
                pageRows.stream().map(mapper::toFeedResponse).toList(),
                next
        );
    }

    /**
     * Отклоняет размер страницы, который лента не обслуживает, до запроса.
     *
     * @param limit запрошенный размер страницы
     * @throws IllegalArgumentException {@code limit} меньше 1 или больше {@link #MAX_LIMIT}
     */
    private static void checkLimit(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException(
                    "Feed limit must be from 1 to %d, got %d".formatted(MAX_LIMIT, limit));
        }
    }
}
