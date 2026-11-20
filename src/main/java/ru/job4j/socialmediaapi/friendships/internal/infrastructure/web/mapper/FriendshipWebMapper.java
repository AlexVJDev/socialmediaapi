package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendCursorResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendPageResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.GetFriendsCommand;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.FriendCursorWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.FriendPageWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.FriendWebResponse;

import java.time.Instant;
import java.util.UUID;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * Преобразования между web DTO {@code FriendshipController} и сценариями: query-параметры → команда,
 * результат → ответ. Web DTO генерируются по OpenAPI-контракту ({@code openapi-generator-maven-plugin}).
 */
@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FriendshipWebMapper {

    /**
     * @param userId         пользователь, чьи друзья читаются
     * @param limit          размер страницы; значение по умолчанию (20) подставляет Spring, поэтому не {@code null}
     * @param afterCreatedAt {@code next.createdAt} предыдущей страницы, {@code null} для первой страницы
     * @param afterId        {@code next.id} предыдущей страницы, {@code null} для первой страницы
     * @return команда сценария
     */
    GetFriendsCommand toCommand(UUID userId, Integer limit, Instant afterCreatedAt, UUID afterId);

    FriendWebResponse toResponse(FriendResult result);

    FriendCursorWebResponse toResponse(FriendCursorResult result);

    FriendPageWebResponse toResponse(FriendPageResult result);
}
