package ru.job4j.socialmediaapi.posts.internal.infrastructure.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.posts.internal.application.model.FeedCursorResult;
import ru.job4j.socialmediaapi.posts.internal.application.model.FeedPageResult;
import ru.job4j.socialmediaapi.posts.internal.application.model.GetFeedCommand;
import ru.job4j.socialmediaapi.posts.internal.application.model.GetPostCommand;
import ru.job4j.socialmediaapi.posts.internal.application.model.PostResult;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.web.request.FeedWebRequest;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.web.response.FeedCursorWebResponse;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.web.response.FeedPageWebResponse;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.web.response.PostWebResponse;

import java.util.UUID;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * {@code status} is returned as a string: MapStruct maps the enum with {@code name()}.
 */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PostWebMapper {

    @Mapping(target = "postId", source = "postId")
    GetPostCommand toGetPostCommand(UUID postId);

    GetFeedCommand toGetFeedCommand(FeedWebRequest request);

    PostWebResponse toResponse(PostResult result);

    FeedPageWebResponse toResponse(FeedPageResult result);

    FeedCursorWebResponse toResponse(FeedCursorResult result);
}
