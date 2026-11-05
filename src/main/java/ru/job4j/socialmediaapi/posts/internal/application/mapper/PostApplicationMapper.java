package ru.job4j.socialmediaapi.posts.internal.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.posts.internal.application.model.FeedCursorResult;
import ru.job4j.socialmediaapi.posts.internal.application.model.FeedPageResult;
import ru.job4j.socialmediaapi.posts.internal.application.model.PostResult;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedCursor;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedPageResponse;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedPostResponse;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.PostResponse;

import java.util.Optional;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * Port responses → scenario results. A post and a post of the feed have the same fields,
 * so both become {@link PostResult}. The empty {@code next} of a page becomes {@code null}.
 */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PostApplicationMapper {

    PostResult toResult(PostResponse response);

    PostResult toResult(FeedPostResponse response);

    FeedPageResult toResult(FeedPageResponse response);

    FeedCursorResult toCursorResult(FeedCursor cursor);

    /**
     * @param next cursor of the next page, empty if there are no more posts
     * @return the cursor, or {@code null} if there are no more posts
     */
    default FeedCursorResult unwrapNext(Optional<FeedCursor> next) {
        return next.map(this::toCursorResult).orElse(null);
    }
}
