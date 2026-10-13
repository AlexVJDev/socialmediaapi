package ru.job4j.socialmediaapi.adapter.in.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.adapter.in.web.dto.CreateOfferFriendshipWebRequest;
import ru.job4j.socialmediaapi.adapter.in.web.dto.CreateOfferFriendshipWebResponse;
import ru.job4j.socialmediaapi.application.port.in.CreateOfferFriendshipUseCase.CreateOfferFriendshipCommand;
import ru.job4j.socialmediaapi.application.port.in.CreateOfferFriendshipUseCase.CreateOfferFriendshipResult.Created;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * {@code status} is returned as a string: MapStruct maps the enum with {@code name()}.
 */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OfferFriendshipWebMapper {

    CreateOfferFriendshipCommand toCommand(CreateOfferFriendshipWebRequest request);

    CreateOfferFriendshipWebResponse toResponse(Created result);
}
