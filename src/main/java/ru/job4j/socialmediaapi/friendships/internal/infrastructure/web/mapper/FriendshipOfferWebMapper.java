package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.friendships.internal.application.model.AcceptFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferDecisionResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.RejectFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.CreateFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.DecideFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.AcceptFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.CreateFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.model.CreateFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.model.CreateFriendshipOfferResult;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.RejectFriendshipOfferWebResponse;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * {@code status} is returned as a string: MapStruct maps the enum with {@code name()}.
 * The decision time of accept/reject is named after the endpoint: {@code acceptedAt} / {@code rejectedAt}.
 */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FriendshipOfferWebMapper {

    CreateFriendshipOfferCommand toCommand(CreateFriendshipOfferWebRequest request);

    CreateFriendshipOfferWebResponse toResponse(CreateFriendshipOfferResult result);

    AcceptFriendshipOfferCommand toAcceptCommand(DecideFriendshipOfferWebRequest request);

    RejectFriendshipOfferCommand toRejectCommand(DecideFriendshipOfferWebRequest request);

    @Mapping(target = "acceptedAt", source = "decidedAt")
    AcceptFriendshipOfferWebResponse toAcceptResponse(FriendshipOfferDecisionResult result);

    @Mapping(target = "rejectedAt", source = "decidedAt")
    RejectFriendshipOfferWebResponse toRejectResponse(FriendshipOfferDecisionResult result);
}
