package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.CreateFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.CreateFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.model.CreateFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.model.CreateFriendshipOfferResult;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * {@code status} is returned as a string: MapStruct maps the enum with {@code name()}.
 */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FriendshipOfferWebMapper {

    CreateFriendshipOfferCommand toCommand(CreateFriendshipOfferWebRequest request);

    CreateFriendshipOfferWebResponse toResponse(CreateFriendshipOfferResult result);
}
