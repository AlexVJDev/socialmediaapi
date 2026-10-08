package ru.job4j.socialmediaapi.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.adapter.out.persistence.entity.OfferFriendshipEntity;
import ru.job4j.socialmediaapi.application.port.out.OfferFriendshipRepository.CreateOfferFriendshipRequest;
import ru.job4j.socialmediaapi.application.port.out.OfferFriendshipRepository.CreateOfferFriendshipResponse;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * {@code status} is stored as a string: MapStruct maps the enum
 * with {@code name()} and back with {@code valueOf()}.
 */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OfferFriendshipPersistenceMapper {

    OfferFriendshipEntity toEntity(CreateOfferFriendshipRequest request);

    CreateOfferFriendshipResponse toResponse(OfferFriendshipEntity entity);
}
