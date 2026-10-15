package ru.job4j.socialmediaapi.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.adapter.out.persistence.row.SubscriptionRow;
import ru.job4j.socialmediaapi.application.port.out.SubscriptionRepository.CreateSubscriptionRequest;
import ru.job4j.socialmediaapi.application.port.out.SubscriptionRepository.CreateSubscriptionResponse;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SubscriptionPersistenceMapper {

    SubscriptionRow toRow(CreateSubscriptionRequest request);

    CreateSubscriptionResponse toResponse(SubscriptionRow row);
}
