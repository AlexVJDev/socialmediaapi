package ru.job4j.socialmediaapi.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.application.port.in.CreateFriendshipOfferUseCase.CreateFriendshipOfferResult;
import ru.job4j.socialmediaapi.application.port.out.FriendshipOfferRepository.CreateFriendshipOfferRequest;
import ru.job4j.socialmediaapi.application.port.out.FriendshipOfferRepository.CreateFriendshipOfferResponse;
import ru.job4j.socialmediaapi.domain.friendshipoffer.FriendshipOffer;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FriendshipOfferApplicationMapper {

    CreateFriendshipOfferRequest toRequest(FriendshipOffer offer);

    CreateFriendshipOfferResult toResult(CreateFriendshipOfferResponse response);
}
