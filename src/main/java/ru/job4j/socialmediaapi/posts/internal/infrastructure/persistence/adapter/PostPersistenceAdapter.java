package ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.job4j.socialmediaapi.posts.internal.application.port.FeedRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.PostRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.CreatePostRequest;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.DeletePostRequest;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedCursor;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedPageResponse;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.PostResponse;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.UpdatePostRequest;
import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.jpa.PostEntity;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.jpa.PostJpaRepository;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.mapper.PostPersistenceMapper;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-реализация порта {@link PostRepository} по таблице {@code posts}: запросы выполняет
 * {@link PostJpaRepository}, сущности преобразуются в модели порта.
 *
 * <p>Посты автора читаются страницами по курсору {@code (created_at, id)} из частичного индекса
 * {@code idx_posts_published_author_created}; лишняя {@code limit + 1}-я строка только сообщает,
 * что есть следующая страница (как в {@link FeedPersistenceAdapter}).
 */
@Component
@RequiredArgsConstructor
public class PostPersistenceAdapter implements PostRepository {

    /** Запросы Spring Data JPA к {@code posts}. */
    private final PostJpaRepository jpaRepository;

    /** Преобразует сущности в модели порта. */
    private final PostPersistenceMapper mapper;

    @Override
    public PostResponse createPost(CreatePostRequest request) {
        PostEntity saved = jpaRepository.save(PostEntity.newPost(
                request.id(),
                request.authorId(),
                request.title(),
                request.content(),
                request.status(),
                request.createdAt(),
                request.updatedAt()
        ));
        return mapper.toResponse(saved);
    }

    @Override
    public Optional<PostResponse> findPublishedById(UUID postId) {
        return jpaRepository.findByIdAndStatus(postId, PostStatus.PUBLISHED).map(mapper::toResponse);
    }

    @Override
    public Optional<PostResponse> updatePost(UpdatePostRequest request) {
        int updated = jpaRepository.updatePublished(
                request.postId(), request.actorId(), request.title(), request.content(), request.updatedAt()
        );
        if (updated == 0) {
            return Optional.empty();
        }
        return jpaRepository.findById(request.postId()).map(mapper::toResponse);
    }

    @Override
    public boolean deletePost(DeletePostRequest request) {
        return jpaRepository.deletePublished(request.postId(), request.actorId(), request.deletedAt()) == 1;
    }

    @Override
    public FeedPageResponse findPublishedByAuthor(UUID authorId, int limit) {
        checkLimit(limit);
        return toPage(jpaRepository.findPublishedByAuthorFirstPage(authorId, limit + 1), limit);
    }

    @Override
    public FeedPageResponse findPublishedByAuthorAfter(UUID authorId, FeedCursor after, int limit) {
        Objects.requireNonNull(after, "after");
        checkLimit(limit);
        return toPage(
                jpaRepository.findPublishedByAuthorPageAfter(authorId, after.createdAt(), after.id(), limit + 1),
                limit
        );
    }

    /**
     * Собирает страницу из до {@code limit + 1} прочитанных строк: лишняя строка не возвращается,
     * она только означает, что есть следующая страница.
     *
     * @param rows  прочитанные публикации
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
     * Отклоняет размер страницы вне допустимого до запроса.
     *
     * @param limit запрошенный размер страницы
     * @throws IllegalArgumentException {@code limit} меньше 1 или больше {@link FeedRepository#MAX_LIMIT}
     */
    private static void checkLimit(int limit) {
        if (limit < 1 || limit > FeedRepository.MAX_LIMIT) {
            throw new IllegalArgumentException(
                    "Page limit must be from 1 to %d, got %d".formatted(FeedRepository.MAX_LIMIT, limit));
        }
    }
}
