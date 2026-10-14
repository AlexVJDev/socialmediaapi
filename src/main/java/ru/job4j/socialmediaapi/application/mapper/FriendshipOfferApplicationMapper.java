package ru.job4j.socialmediaapi.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.application.port.in.CreateFriendshipOfferUseCase.CreateFriendshipOfferResult.Created;
import ru.job4j.socialmediaapi.application.port.out.FriendshipOfferRepository.CreateFriendshipOfferResponse;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FriendshipOfferApplicationMapper {

    Created toResult(CreateFriendshipOfferResponse response);
}
