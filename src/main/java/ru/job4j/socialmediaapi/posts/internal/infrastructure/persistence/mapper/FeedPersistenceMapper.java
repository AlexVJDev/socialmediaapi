package ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.row.PostRow;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedPostResponse;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * {@code status} is stored as a string: MapStruct maps it back to the enum with {@code valueOf()}.
 */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FeedPersistenceMapper {

    FeedPostResponse toResponse(PostRow row);
}
