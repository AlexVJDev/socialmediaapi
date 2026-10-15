package ru.job4j.socialmediaapi.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.adapter.out.persistence.row.FriendshipOfferRow;
import ru.job4j.socialmediaapi.application.port.out.FriendshipOfferRepository.CreateFriendshipOfferRequest;
import ru.job4j.socialmediaapi.application.port.out.FriendshipOfferRepository.CreateFriendshipOfferResponse;
import ru.job4j.socialmediaapi.application.port.out.FriendshipOfferRepository.FindFriendshipOfferResponse;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * {@code status} is stored as a string: MapStruct maps the enum
 * with {@code name()} and back with {@code valueOf()}.
 */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FriendshipOfferPersistenceMapper {

    FriendshipOfferRow toRow(CreateFriendshipOfferRequest request);

    CreateFriendshipOfferResponse toResponse(FriendshipOfferRow row);

    FindFriendshipOfferResponse toFindResponse(FriendshipOfferRow row);
}
