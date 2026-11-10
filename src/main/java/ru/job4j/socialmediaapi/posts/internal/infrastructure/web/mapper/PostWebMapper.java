package ru.job4j.socialmediaapi.posts.internal.infrastructure.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;
import ru.job4j.socialmediaapi.posts.internal.application.model.FeedCursorResult;
import ru.job4j.socialmediaapi.posts.internal.application.model.FeedPageResult;
import ru.job4j.socialmediaapi.posts.internal.application.model.GetFeedCommand;
import ru.job4j.socialmediaapi.posts.internal.application.model.GetPostCommand;
import ru.job4j.socialmediaapi.posts.internal.application.model.PostResult;
import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.web.response.FeedCursorWebResponse;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.web.response.FeedPageWebResponse;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.web.response.PostWebResponse;

import java.time.Instant;
import java.util.UUID;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * Maps between the web DTOs of {@code PostController} / {@code FeedController} and the scenarios.
 * <p>
 * The {@code to*Response} methods play the role of a Presenter: they know the shape of the external
 * response and keep the controllers away from the details of building it (enum mapping, the optional
 * cursor, filling the DTO). The mapper holds no business rules and does not call repositories.
 * The {@code to*Command} methods do the opposite direction: path and query parameters → scenario command;
 * they come as separate arguments of the generated interfaces.
 * <p>
 * Web DTOs are generated from the OpenAPI contract ({@code openapi-generator-maven-plugin}); the internal
 * {@code PostResult} / {@code FeedPageResult} do not depend on them.
 * The contract allows only {@code PUBLISHED} posts in responses: a deleted post is never returned,
 * so any other status is a bug and throws {@link IllegalArgumentException}.
 */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PostWebMapper {

    @Mapping(target = "postId", source = "postId")
    GetPostCommand toGetPostCommand(UUID postId);

    /**
     * @param followerId     reader of the feed
     * @param limit          page size; the contract default (20) is set by Spring, so it is never {@code null}
     * @param afterCreatedAt {@code next.createdAt} of the previous page, {@code null} for the first page
     * @param afterId        {@code next.id} of the previous page, {@code null} for the first page
     * @return the command of the scenario
     */
    GetFeedCommand toGetFeedCommand(UUID followerId, Integer limit, Instant afterCreatedAt, UUID afterId);

    PostWebResponse toResponse(PostResult result);

    FeedPageWebResponse toResponse(FeedPageResult result);

    FeedCursorWebResponse toResponse(FeedCursorResult result);

    /**
     * @param status status of a returned post
     * @return {@code PUBLISHED}; any other status throws {@link IllegalArgumentException}
     */
    @ValueMapping(source = "PUBLISHED", target = "PUBLISHED")
    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.THROW_EXCEPTION)
    PostWebResponse.StatusEnum toStatus(PostStatus status);
}
