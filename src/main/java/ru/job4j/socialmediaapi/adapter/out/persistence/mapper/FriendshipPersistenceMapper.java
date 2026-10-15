package ru.job4j.socialmediaapi.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.adapter.out.persistence.row.FriendshipsRow;
import ru.job4j.socialmediaapi.application.port.out.FriendshipsRepository.CreateFriendshipRequest;
import ru.job4j.socialmediaapi.application.port.out.FriendshipsRepository.CreateFriendshipResponse;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FriendshipPersistenceMapper {

    FriendshipsRow toRow(CreateFriendshipRequest request);

    CreateFriendshipResponse toResponse(FriendshipsRow row);
}
