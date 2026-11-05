package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;
import ru.job4j.socialmediaapi.friendships.internal.application.model.AcceptFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.model.CreateFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.model.CreateFriendshipOfferResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferDecisionResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferPageResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.GetFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.model.GetFriendshipOffersCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.model.RejectFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.AcceptFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.CreateFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.RejectFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.AcceptFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.CreateFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.FriendshipOfferPageWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.FriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.RejectFriendshipOfferWebResponse;

import java.time.Instant;
import java.util.UUID;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * Maps between the web DTOs of {@code FriendshipOfferController} and the scenarios.
 * <p>
 * The {@code to*Response} methods play the role of a Presenter: they know the shape of the external
 * response and keep the controller away from the details of building it (enum mapping, field renaming,
 * filling the DTO). The mapper holds no business rules and does not call repositories.
 * The {@code to*Command} methods do the opposite direction: web request → scenario command.
 * <p>
 * Web DTOs are generated from the OpenAPI contract ({@code openapi-generator-maven-plugin}).
 * {@code status} is mapped by name: domain {@code FriendshipOfferStatus} → {@code StatusEnum} of the response.
 * The responses of create, accept and reject allow only the status the operation leads to
 * ({@code PENDING}, {@code ACCEPTED}, {@code REJECTED}); any other one is a bug and throws
 * {@link IllegalArgumentException} instead of an answer that breaks the contract.
 * The decision time of accept/reject is named after the endpoint: {@code acceptedAt} / {@code rejectedAt}.
 * Query and path parameters of the GET endpoints come as separate arguments of the generated interface.
 */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FriendshipOfferWebMapper {

    CreateFriendshipOfferCommand toCommand(CreateFriendshipOfferWebRequest request);

    CreateFriendshipOfferWebResponse toResponse(CreateFriendshipOfferResult result);

    AcceptFriendshipOfferCommand toAcceptCommand(AcceptFriendshipOfferWebRequest request);

    RejectFriendshipOfferCommand toRejectCommand(RejectFriendshipOfferWebRequest request);

    @Mapping(target = "acceptedAt", source = "decidedAt")
    AcceptFriendshipOfferWebResponse toAcceptResponse(FriendshipOfferDecisionResult result);

    @Mapping(target = "rejectedAt", source = "decidedAt")
    RejectFriendshipOfferWebResponse toRejectResponse(FriendshipOfferDecisionResult result);

    @Mapping(target = "offerId", source = "offerId")
    GetFriendshipOfferCommand toGetCommand(UUID offerId);

    /**
     * @param limit          page size; the contract default (20) is set by Spring, so it is never {@code null}
     * @param afterCreatedAt {@code next.createdAt} of the previous page, {@code null} for the first page
     * @param afterId        {@code next.id} of the previous page, {@code null} for the first page
     * @return the command of the scenario
     */
    GetFriendshipOffersCommand toGetPageCommand(Integer limit, Instant afterCreatedAt, UUID afterId);

    FriendshipOfferWebResponse toResponse(FriendshipOfferResult result);

    FriendshipOfferPageWebResponse toResponse(FriendshipOfferPageResult result);

    /**
     * @param status status of the created offer
     * @return {@code PENDING}; any other status throws {@link IllegalArgumentException}
     */
    @ValueMapping(source = "PENDING", target = "PENDING")
    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.THROW_EXCEPTION)
    CreateFriendshipOfferWebResponse.StatusEnum toCreateStatus(FriendshipOfferStatus status);

    /**
     * @param status status of the accepted offer
     * @return {@code ACCEPTED}; any other status throws {@link IllegalArgumentException}
     */
    @ValueMapping(source = "ACCEPTED", target = "ACCEPTED")
    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.THROW_EXCEPTION)
    AcceptFriendshipOfferWebResponse.StatusEnum toAcceptStatus(FriendshipOfferStatus status);

    /**
     * @param status status of the rejected offer
     * @return {@code REJECTED}; any other status throws {@link IllegalArgumentException}
     */
    @ValueMapping(source = "REJECTED", target = "REJECTED")
    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.THROW_EXCEPTION)
    RejectFriendshipOfferWebResponse.StatusEnum toRejectStatus(FriendshipOfferStatus status);
}
