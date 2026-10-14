package ru.job4j.socialmediaapi.adapter.in.web.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.job4j.socialmediaapi.adapter.in.web.dto.CreateOfferFriendshipWebRequest;
import ru.job4j.socialmediaapi.adapter.in.web.dto.CreateOfferFriendshipWebResponse;
import ru.job4j.socialmediaapi.application.port.in.CreateOfferFriendshipUseCase.CreateOfferFriendshipCommand;
import ru.job4j.socialmediaapi.application.port.in.CreateOfferFriendshipUseCase.CreateOfferFriendshipResult.Created;
import ru.job4j.socialmediaapi.domain.offerfriendship.OfferFriendshipStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Web-маппер заявок в друзья: преобразование между web-DTO и входным портом")
class OfferFriendshipWebMapperTest {

    /** Fixed creation time of the offer. */
    private static final Instant CREATED_AT = Instant.parse("2026-10-12T10:15:00Z");

    /** Mapper under test, created without Spring. */
    private final OfferFriendshipWebMapper mapper =
            Mappers.getMapper(OfferFriendshipWebMapper.class);

    @Test
    @DisplayName("toCommand: идентификаторы пользователей переносятся в команду")
    void whenToCommandThenCopyUserIds() {
        var request = new CreateOfferFriendshipWebRequest(UUID.randomUUID(), UUID.randomUUID());

        var command = mapper.toCommand(request);

        assertThat(command).isEqualTo(new CreateOfferFriendshipCommand(
                request.fromUserId(), request.toUserId()
        ));
    }

    @Test
    @DisplayName("toResponse: статус-enum возвращается строкой через name()")
    void whenToResponseThenStatusAsName() {
        var created = new Created(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                OfferFriendshipStatus.PENDING, CREATED_AT
        );

        var response = mapper.toResponse(created);

        assertThat(response).isEqualTo(new CreateOfferFriendshipWebResponse(
                created.id(), created.fromUserId(), created.toUserId(), "PENDING", CREATED_AT
        ));
    }
}
