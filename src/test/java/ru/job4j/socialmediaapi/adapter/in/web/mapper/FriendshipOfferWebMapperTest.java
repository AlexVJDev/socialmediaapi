package ru.job4j.socialmediaapi.adapter.in.web.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.job4j.socialmediaapi.adapter.in.web.dto.CreateFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.adapter.in.web.dto.CreateFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.application.port.in.CreateFriendshipOfferUseCase.CreateFriendshipOfferUseCaseRequest;
import ru.job4j.socialmediaapi.application.port.in.CreateFriendshipOfferUseCase.CreateFriendshipOfferResult;
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
    @DisplayName("toUseCaseRequest: идентификаторы пользователей переносятся в запрос use case")
    void whenToUseCaseRequestThenCopyUserIds() {
        var request = new CreateFriendshipOfferWebRequest(UUID.randomUUID(), UUID.randomUUID());

        var useCaseRequest = mapper.toUseCaseRequest(request);

        assertThat(useCaseRequest).isEqualTo(new CreateFriendshipOfferUseCaseRequest(
                request.fromUserId(), request.toUserId()
        ));
    }

    @Test
    @DisplayName("toResponse: статус-enum возвращается строкой через name()")
    void whenToResponseThenStatusAsName() {
        var created = new CreateFriendshipOfferResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                FriendshipOfferStatus.PENDING, CREATED_AT
        );

        var response = mapper.toResponse(created);

        assertThat(response).isEqualTo(new CreateFriendshipOfferWebResponse(
                created.id(), created.fromUserId(), created.toUserId(), "PENDING", CREATED_AT
        ));
    }
}
