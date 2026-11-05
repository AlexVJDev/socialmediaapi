package ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedCursor;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedPostResponse;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.row.PostRow;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * Maps feed rows to the port models. {@code status} is stored as a string: MapStruct maps it
 * back to the enum with {@code valueOf()}. {@link #toCursor} takes the position of a row
 * in the feed — its {@code createdAt} and {@code id}.
 */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FeedPersistenceMapper {

    /**
     * Post of a feed page.
     *
     * @param row stored post
     * @return the post for the port
     * @throws IllegalArgumentException the stored status is unknown
     */
    FeedPostResponse toResponse(PostRow row);

    /**
     * Position of the post in the feed, used as the cursor of the next page.
     *
     * @param row stored post
     * @return the cursor pointing at this post
     */
    FeedCursor toCursor(PostRow row);
}
