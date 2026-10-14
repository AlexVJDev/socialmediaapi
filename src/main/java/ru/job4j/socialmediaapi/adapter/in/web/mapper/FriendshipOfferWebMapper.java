package ru.job4j.socialmediaapi.adapter.in.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.adapter.in.web.dto.CreateFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.adapter.in.web.dto.CreateFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.application.port.in.CreateFriendshipOfferUseCase.CreateFriendshipOfferCommand;
import ru.job4j.socialmediaapi.application.port.in.CreateFriendshipOfferUseCase.CreateFriendshipOfferResult.Created;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * {@code status} is returned as a string: MapStruct maps the enum with {@code name()}.
 */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FriendshipOfferWebMapper {

    CreateFriendshipOfferCommand toCommand(CreateFriendshipOfferWebRequest request);

    CreateFriendshipOfferWebResponse toResponse(Created result);
}
