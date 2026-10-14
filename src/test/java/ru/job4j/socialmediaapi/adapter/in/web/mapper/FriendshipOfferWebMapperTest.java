package ru.job4j.socialmediaapi.adapter.in.web.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.job4j.socialmediaapi.adapter.in.web.dto.CreateFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.adapter.in.web.dto.CreateFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.application.port.in.CreateFriendshipOfferUseCase.CreateFriendshipOfferCommand;
import ru.job4j.socialmediaapi.application.port.in.CreateFriendshipOfferUseCase.CreateFriendshipOfferResult.Created;
import ru.job4j.socialmediaapi.domain.friendshipoffer.FriendshipOfferStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Web-маппер заявок в друзья: преобразование между web-DTO и входным портом")
class FriendshipOfferWebMapperTest {

    /** Fixed creation time of the offer. */
    private static final Instant CREATED_AT = Instant.parse("2026-10-12T10:15:00Z");

    /** Mapper under test, created without Spring. */
    private final FriendshipOfferWebMapper mapper =
            Mappers.getMapper(FriendshipOfferWebMapper.class);

    @Test
    @DisplayName("toCommand: идентификаторы пользователей переносятся в команду")
    void whenToCommandThenCopyUserIds() {
        var request = new CreateFriendshipOfferWebRequest(UUID.randomUUID(), UUID.randomUUID());

        var command = mapper.toCommand(request);

        assertThat(command).isEqualTo(new CreateFriendshipOfferCommand(
                request.fromUserId(), request.toUserId()
        ));
    }

    @Test
    @DisplayName("toResponse: статус-enum возвращается строкой через name()")
    void whenToResponseThenStatusAsName() {
        var created = new Created(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                FriendshipOfferStatus.PENDING, CREATED_AT
        );

        var response = mapper.toResponse(created);

        assertThat(response).isEqualTo(new CreateFriendshipOfferWebResponse(
                created.id(), created.fromUserId(), created.toUserId(), "PENDING", CREATED_AT
        ));
    }
}
