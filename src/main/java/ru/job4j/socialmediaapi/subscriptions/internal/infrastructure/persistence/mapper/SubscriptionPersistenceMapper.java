package ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.subscriptions.internal.application.port.model.SubscriptionResponse;
import ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.persistence.jpa.SubscriptionEntity;

import java.util.List;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * Преобразует JPA-сущность подписки в модель порта {@code SubscriptionRepository}.
 */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SubscriptionPersistenceMapper {

    /**
     * @param entity подписка
     * @return подписка в модели порта
     */
    SubscriptionResponse toResponse(SubscriptionEntity entity);

    /**
     * @param entities подписки
     * @return подписки в модели порта, в том же порядке
     */
    List<SubscriptionResponse> toResponses(List<SubscriptionEntity> entities);
}
