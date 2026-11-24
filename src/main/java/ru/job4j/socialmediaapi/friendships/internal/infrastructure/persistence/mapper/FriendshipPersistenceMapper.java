package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FindFriendResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendCursor;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.jpa.FriendView;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.jpa.FriendshipEntity;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * Преобразует JPA-сущность дружбы и строки страницы друзей в модели порта {@code FriendshipsRepository}.
 */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FriendshipPersistenceMapper {

    /**
     * @param entity вставленная дружба
     * @return ответ на создание дружбы
     */
    CreateFriendshipResponse toResponse(FriendshipEntity entity);

    /**
     * @param view строка страницы друзей
     * @return друг: id дружбы и второй участник
     */
    @Mapping(target = "userId", source = "friendId")
    FindFriendResponse toFindResponse(FriendView view);

    /**
     * @param view строка страницы друзей
     * @return её позиция в порядке страницы: {@code created_at} и {@code id}
     */
    FriendCursor toCursor(FriendView view);
}
