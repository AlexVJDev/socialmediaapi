package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.job4j.socialmediaapi.friendships.internal.application.model.AcceptFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferDecisionResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.RejectFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.CreateFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.DecideFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.AcceptFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.RejectFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.CreateFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.model.CreateFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.model.CreateFriendshipOfferResult;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Web-маппер заявок в друзья: преобразование между web-DTO и входным портом")
class FriendshipOfferWebMapperTest {

    /** Fixed creation time of the offer. */
    private static final Instant CREATED_AT = Instant.parse("2026-10-12T10:15:00Z");

    /** Fixed decision time of the offer. */
    private static final Instant DECIDED_AT = Instant.parse("2026-10-12T10:20:00Z");

    /** Mapper under test, created without Spring. */
    private final FriendshipOfferWebMapper mapper =
            Mappers.getMapper(FriendshipOfferWebMapper.class);

    @Test
    @DisplayName("toCommand: идентификаторы пользователей переносятся в команду сценария")
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
        var created = new CreateFriendshipOfferResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                FriendshipOfferStatus.PENDING, CREATED_AT
        );

        var response = mapper.toResponse(created);

        assertThat(response).isEqualTo(new CreateFriendshipOfferWebResponse(
                created.id(), created.fromUserId(), created.toUserId(), "PENDING", CREATED_AT
        ));
    }

    @Test
    @DisplayName("toAcceptCommand / toRejectCommand: id заявки и действующий пользователь переносятся в команду")
    void whenToDecisionCommandThenCopyIds() {
        var request = new DecideFriendshipOfferWebRequest(UUID.randomUUID(), UUID.randomUUID());

        assertThat(mapper.toAcceptCommand(request))
                .isEqualTo(new AcceptFriendshipOfferCommand(request.offerId(), request.actorId()));
        assertThat(mapper.toRejectCommand(request))
                .isEqualTo(new RejectFriendshipOfferCommand(request.offerId(), request.actorId()));
    }

    @Test
    @DisplayName("toAcceptResponse: decidedAt становится acceptedAt, статус — строкой")
    void whenToAcceptResponseThenDecidedAtAsAcceptedAt() {
        var result = decision(FriendshipOfferStatus.ACCEPTED);

        assertThat(mapper.toAcceptResponse(result)).isEqualTo(new AcceptFriendshipOfferWebResponse(
                result.id(), result.fromUserId(), result.toUserId(), "ACCEPTED", CREATED_AT, DECIDED_AT
        ));
    }

    @Test
    @DisplayName("toRejectResponse: decidedAt становится rejectedAt, статус — строкой")
    void whenToRejectResponseThenDecidedAtAsRejectedAt() {
        var result = decision(FriendshipOfferStatus.REJECTED);

        assertThat(mapper.toRejectResponse(result)).isEqualTo(new RejectFriendshipOfferWebResponse(
                result.id(), result.fromUserId(), result.toUserId(), "REJECTED", CREATED_AT, DECIDED_AT
        ));
    }

    private static FriendshipOfferDecisionResult decision(FriendshipOfferStatus status) {
        return new FriendshipOfferDecisionResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), status, CREATED_AT, DECIDED_AT
        );
    }
}
