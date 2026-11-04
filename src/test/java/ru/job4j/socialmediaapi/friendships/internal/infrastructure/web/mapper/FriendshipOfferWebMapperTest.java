package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.job4j.socialmediaapi.friendships.internal.application.model.AcceptFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferCursorResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferDecisionResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferPageResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.GetFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.model.GetFriendshipOffersCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.model.RejectFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.CreateFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.AcceptFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.RejectFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.AcceptFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.RejectFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.CreateFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.FriendshipOfferCursorWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.FriendshipOfferPageWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.FriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.model.CreateFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.model.CreateFriendshipOfferResult;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

import java.time.Instant;
import java.util.List;
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
                request.getFromUserId(), request.getToUserId()
        ));
    }

    @Test
    @DisplayName("toResponse: доменный статус становится StatusEnum ответа по имени")
    void whenToResponseThenStatusAsName() {
        var created = new CreateFriendshipOfferResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                FriendshipOfferStatus.PENDING, CREATED_AT
        );

        var response = mapper.toResponse(created);

        assertThat(response).isEqualTo(new CreateFriendshipOfferWebResponse(
                created.id(), created.fromUserId(), created.toUserId(),
                CreateFriendshipOfferWebResponse.StatusEnum.PENDING, CREATED_AT
        ));
    }

    @Test
    @DisplayName("toAcceptCommand / toRejectCommand: id заявки и действующий пользователь переносятся в команду")
    void whenToDecisionCommandThenCopyIds() {
        var offerId = UUID.randomUUID();
        var actorId = UUID.randomUUID();

        assertThat(mapper.toAcceptCommand(new AcceptFriendshipOfferWebRequest(offerId, actorId)))
                .isEqualTo(new AcceptFriendshipOfferCommand(offerId, actorId));
        assertThat(mapper.toRejectCommand(new RejectFriendshipOfferWebRequest(offerId, actorId)))
                .isEqualTo(new RejectFriendshipOfferCommand(offerId, actorId));
    }

    @Test
    @DisplayName("toAcceptResponse: decidedAt становится acceptedAt, статус — StatusEnum")
    void whenToAcceptResponseThenDecidedAtAsAcceptedAt() {
        var result = decision(FriendshipOfferStatus.ACCEPTED);

        assertThat(mapper.toAcceptResponse(result)).isEqualTo(new AcceptFriendshipOfferWebResponse(
                result.id(), result.fromUserId(), result.toUserId(),
                AcceptFriendshipOfferWebResponse.StatusEnum.ACCEPTED, CREATED_AT, DECIDED_AT
        ));
    }

    @Test
    @DisplayName("toRejectResponse: decidedAt становится rejectedAt, статус — StatusEnum")
    void whenToRejectResponseThenDecidedAtAsRejectedAt() {
        var result = decision(FriendshipOfferStatus.REJECTED);

        assertThat(mapper.toRejectResponse(result)).isEqualTo(new RejectFriendshipOfferWebResponse(
                result.id(), result.fromUserId(), result.toUserId(),
                RejectFriendshipOfferWebResponse.StatusEnum.REJECTED, CREATED_AT, DECIDED_AT
        ));
    }

    @Test
    @DisplayName("toGetCommand / toGetPageCommand: параметры пути и запроса переносятся в команды")
    void whenToGetCommandsThenCopyParameters() {
        var id = UUID.randomUUID();

        assertThat(mapper.toGetCommand(id)).isEqualTo(new GetFriendshipOfferCommand(id));
        assertThat(mapper.toGetPageCommand(5, CREATED_AT, id))
                .isEqualTo(new GetFriendshipOffersCommand(CREATED_AT, id, 5));
    }

    @Test
    @DisplayName("toResponse страницы: заявки со статусом-StatusEnum и курсор; null next остаётся null")
    void whenToPageResponseThenOffersAndNext() {
        var offer = new FriendshipOfferResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                FriendshipOfferStatus.ACCEPTED, CREATED_AT, DECIDED_AT
        );
        var expectedOffer = new FriendshipOfferWebResponse(
                offer.id(), offer.fromUserId(), offer.toUserId(),
                FriendshipOfferWebResponse.StatusEnum.ACCEPTED, CREATED_AT, DECIDED_AT
        );

        var withNext = mapper.toResponse(new FriendshipOfferPageResult(
                List.of(offer), new FriendshipOfferCursorResult(CREATED_AT, offer.id())));
        var last = mapper.toResponse(new FriendshipOfferPageResult(List.of(offer), null));

        assertThat(withNext).isEqualTo(new FriendshipOfferPageWebResponse(List.of(expectedOffer),
                new FriendshipOfferCursorWebResponse(CREATED_AT, offer.id())));
        assertThat(last.getOffers()).containsExactly(expectedOffer);
        assertThat(last.getNext()).isNull();
    }

    private static FriendshipOfferDecisionResult decision(FriendshipOfferStatus status) {
        return new FriendshipOfferDecisionResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), status, CREATED_AT, DECIDED_AT
        );
    }
}
