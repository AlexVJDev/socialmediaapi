package ru.job4j.socialmediaapi.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.adapter.out.persistence.entity.FriendshipEntity;
import ru.job4j.socialmediaapi.application.port.out.FriendshipRepository.CreateFriendshipRequest;
import ru.job4j.socialmediaapi.application.port.out.FriendshipRepository.CreateFriendshipResponse;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FriendshipPersistenceMapper {

    FriendshipEntity toEntity(CreateFriendshipRequest request);

    CreateFriendshipResponse toResponse(FriendshipEntity entity);
}
