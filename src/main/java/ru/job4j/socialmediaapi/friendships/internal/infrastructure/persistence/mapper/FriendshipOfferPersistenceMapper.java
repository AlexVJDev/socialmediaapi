package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.row.FriendshipOfferRow;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FindFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.UpdateFriendshipOfferStatusResponse;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * {@code status} is stored as a string: MapStruct maps the enum
 * with {@code name()} and back with {@code valueOf()}.
 */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FriendshipOfferPersistenceMapper {

    FriendshipOfferRow toRow(CreateFriendshipOfferRequest request);

    CreateFriendshipOfferResponse toResponse(FriendshipOfferRow row);

    FindFriendshipOfferResponse toFindResponse(FriendshipOfferRow row);

    UpdateFriendshipOfferStatusResponse toUpdateResponse(FriendshipOfferRow row);

    /**
     * @param status status of the offer
     * @return the status as stored in the {@code status} column
     */
    default String toStatus(FriendshipOfferStatus status) {
        return status.name();
    }
}
