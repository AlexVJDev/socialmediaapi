package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FindFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendshipOfferCursor;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.UpdateFriendshipOfferStatusResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.jpa.FriendshipOfferEntity;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * Преобразует JPA-сущность заявки в модели порта {@code FriendshipOfferRepository}.
 * Статус в сущности уже доменный enum, преобразование не нужно.
 */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FriendshipOfferPersistenceMapper {

    /**
     * @param entity вставленная заявка
     * @return ответ на создание заявки
     */
    CreateFriendshipOfferResponse toResponse(FriendshipOfferEntity entity);

    /**
     * @param entity найденная заявка
     * @return ответ на поиск заявки
     */
    FindFriendshipOfferResponse toFindResponse(FriendshipOfferEntity entity);

    /**
     * @param entity заявка после смены статуса
     * @return ответ на смену статуса
     */
    UpdateFriendshipOfferStatusResponse toUpdateResponse(FriendshipOfferEntity entity);

    /**
     * @param entity заявка страницы
     * @return её позиция в порядке страницы заявок: {@code created_at} и {@code id}
     */
    FriendshipOfferCursor toCursor(FriendshipOfferEntity entity);
}
