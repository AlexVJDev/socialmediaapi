package ru.job4j.socialmediaapi.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.adapter.out.persistence.row.PostRow;
import ru.job4j.socialmediaapi.application.port.out.PostRepository.CreatePostRequest;
import ru.job4j.socialmediaapi.application.port.out.PostRepository.PostResponse;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * {@code status} is stored as a string: MapStruct maps the enum
 * with {@code name()} and back with {@code valueOf()}.
 */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PostPersistenceMapper {

    PostRow toRow(CreatePostRequest request);

    PostResponse toResponse(PostRow row);
}
