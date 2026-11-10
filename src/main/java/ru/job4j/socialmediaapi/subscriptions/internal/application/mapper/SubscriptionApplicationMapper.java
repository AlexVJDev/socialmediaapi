package ru.job4j.socialmediaapi.subscriptions.internal.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.subscriptions.internal.application.model.SubscriptionResult;
import ru.job4j.socialmediaapi.subscriptions.internal.application.port.model.SubscriptionResponse;

import java.util.List;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * Port responses of {@code SubscriptionRepository} → results of the reading scenarios.
 */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SubscriptionApplicationMapper {

    SubscriptionResult toResult(SubscriptionResponse response);

    List<SubscriptionResult> toResults(List<SubscriptionResponse> responses);
}
