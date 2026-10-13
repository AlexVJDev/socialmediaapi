package ru.job4j.socialmediaapi.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.application.port.in.CreateOfferFriendshipUseCase.CreateOfferFriendshipResult.Created;
import ru.job4j.socialmediaapi.application.port.out.OfferFriendshipRepository.CreateOfferFriendshipResponse;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OfferFriendshipApplicationMapper {

    Created toResult(CreateOfferFriendshipResponse response);
}
