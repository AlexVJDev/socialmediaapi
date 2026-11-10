package ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.persistence.row.SubscriptionRow;
import ru.job4j.socialmediaapi.subscriptions.internal.application.port.model.CreateSubscriptionRequest;
import ru.job4j.socialmediaapi.subscriptions.internal.application.port.model.SubscriptionResponse;

import java.util.List;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SubscriptionPersistenceMapper {

    SubscriptionRow toRow(CreateSubscriptionRequest request);

    SubscriptionResponse toResponse(SubscriptionRow row);

    List<SubscriptionResponse> toResponses(List<SubscriptionRow> rows);
}
