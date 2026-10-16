package ru.job4j.socialmediaapi.friendships.internal.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.friendships.internal.application.model.CreateFriendshipOfferResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferDecisionResult;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FindFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.UpdateFriendshipOfferStatusResponse;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOffer;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FriendshipOfferApplicationMapper {

    CreateFriendshipOfferRequest toRequest(FriendshipOffer offer);

    CreateFriendshipOfferResult toResult(CreateFriendshipOfferResponse response);

    /**
     * @param response offer as stored after the decision
     * @return the result of accept/reject; {@code decidedAt} is the time of the status change
     */
    @Mapping(target = "decidedAt", source = "updatedAt")
    FriendshipOfferDecisionResult toResult(UpdateFriendshipOfferStatusResponse response);

    /**
     * Restores the domain offer from the stored one (its invariants are checked again).
     * MapStruct takes {@code accept(Instant)} / {@code reject(Instant)} for fluent setters
     * (they return {@code FriendshipOffer}), so they are ignored explicitly: the record is built
     * through its constructor.
     *
     * @param response stored offer
     * @return the domain offer
     */
    @Mapping(target = "accept", ignore = true)
    @Mapping(target = "reject", ignore = true)
    FriendshipOffer toDomain(FindFriendshipOfferResponse response);
}
