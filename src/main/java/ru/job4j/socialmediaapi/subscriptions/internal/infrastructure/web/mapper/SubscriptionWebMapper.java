package ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.subscriptions.internal.application.model.GetFollowersCommand;
import ru.job4j.socialmediaapi.subscriptions.internal.application.model.GetSubscriptionsCommand;
import ru.job4j.socialmediaapi.subscriptions.internal.application.model.SubscriptionResult;
import ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.web.response.FollowerWebResponse;
import ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.web.response.SubscriptionWebResponse;

import java.util.List;
import java.util.UUID;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * Maps between the web DTOs of {@code SubscriptionController} and the scenarios.
 * <p>
 * The {@code to*Response} methods play the role of a Presenter: they know the shape of the external
 * response and keep the controller away from the details of building it (choosing the other side of
 * the subscription, filling the DTO). The mapper holds no business rules and does not call repositories.
 * The {@code to*Command} methods do the opposite direction: query parameter → scenario command.
 * <p>
 * Web DTOs are generated from the OpenAPI contract ({@code openapi-generator-maven-plugin}); the internal
 * {@code SubscriptionResult} does not depend on them. Both responses carry only the fields of the contract:
 * the subscription {@code id} and {@code userId} — the followed user in the list of subscriptions,
 * the follower in the list of followers; {@code createdAt} and the stored row stay inside.
 */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SubscriptionWebMapper {

    @Mapping(target = "userId", source = "userId")
    GetSubscriptionsCommand toSubscriptionsCommand(UUID userId);

    @Mapping(target = "userId", source = "userId")
    GetFollowersCommand toFollowersCommand(UUID userId);

    @Mapping(target = "userId", source = "followedId")
    SubscriptionWebResponse toSubscriptionResponse(SubscriptionResult result);

    List<SubscriptionWebResponse> toSubscriptionResponses(List<SubscriptionResult> results);

    @Mapping(target = "userId", source = "followerId")
    FollowerWebResponse toFollowerResponse(SubscriptionResult result);

    List<FollowerWebResponse> toFollowerResponses(List<SubscriptionResult> results);
}
