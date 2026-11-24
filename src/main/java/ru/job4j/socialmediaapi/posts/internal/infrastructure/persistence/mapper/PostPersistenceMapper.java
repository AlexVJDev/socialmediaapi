package ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedCursor;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedPostResponse;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.PostResponse;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.jpa.PostEntity;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * Преобразует JPA-сущность публикации в модели порта {@code PostRepository}:
 * публикацию и строку страницы постов автора с её курсором.
 */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PostPersistenceMapper {

    /**
     * @param entity публикация
     * @return публикация в модели порта
     */
    PostResponse toResponse(PostEntity entity);

    /**
     * @param entity публикация страницы постов автора
     * @return пост страницы
     */
    FeedPostResponse toFeedResponse(PostEntity entity);

    /**
     * @param entity публикация страницы постов автора
     * @return её позиция в порядке страницы: {@code created_at} и {@code id}
     */
    FeedCursor toCursor(PostEntity entity);
}
