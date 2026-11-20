package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.row.FriendshipOfferRow;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FindFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendshipOfferCursor;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.UpdateFriendshipOfferStatusResponse;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * {@code status} хранится строкой: MapStruct преобразует enum
 * через {@code name()} и обратно через {@code valueOf()}.
 */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FriendshipOfferPersistenceMapper {

    FriendshipOfferRow toRow(CreateFriendshipOfferRequest request);

    CreateFriendshipOfferResponse toResponse(FriendshipOfferRow row);

    FindFriendshipOfferResponse toFindResponse(FriendshipOfferRow row);

    UpdateFriendshipOfferStatusResponse toUpdateResponse(FriendshipOfferRow row);

    /**
     * @param row заявка страницы
     * @return её позиция в порядке страницы заявок: {@code created_at} и {@code id}
     */
    FriendshipOfferCursor toCursor(FriendshipOfferRow row);

    /**
     * @param status статус заявки
     * @return статус в том виде, как он хранится в колонке {@code status}
     */
    default String toStatus(FriendshipOfferStatus status) {
        return status.name();
    }
}
