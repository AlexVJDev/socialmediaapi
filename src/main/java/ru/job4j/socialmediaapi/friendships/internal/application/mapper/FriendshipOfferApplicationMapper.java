package ru.job4j.socialmediaapi.friendships.internal.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.friendships.internal.application.model.CreateFriendshipOfferResult;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOffer;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FriendshipOfferApplicationMapper {

    CreateFriendshipOfferRequest toRequest(FriendshipOffer offer);

    CreateFriendshipOfferResult toResult(CreateFriendshipOfferResponse response);
}
