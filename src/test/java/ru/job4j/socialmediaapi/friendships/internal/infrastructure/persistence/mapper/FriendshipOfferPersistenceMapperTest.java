package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.row.FriendshipOfferRow;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipOfferRepository.CreateFriendshipOfferRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipOfferRepository.CreateFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipOfferRepository.FindFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Маппер заявок в друзья: преобразование между Request/Response порта и FriendshipOfferRow")
class FriendshipOfferPersistenceMapperTest {

    /** Fixed creation time of the offer. */
    private static final Instant CREATED_AT = Instant.parse("2026-10-06T10:15:00Z");

    /** Fixed update time of the offer. */
    private static final Instant UPDATED_AT = Instant.parse("2026-10-07T11:20:00Z");

    /** Mapper under test, created without Spring. */
    private final FriendshipOfferPersistenceMapper mapper =
            Mappers.getMapper(FriendshipOfferPersistenceMapper.class);

    @Test
    @DisplayName("toRow: статус-enum записывается в Row строкой через name()")
    void whenToRowThenStatusStoredAsName() {
        var request = new CreateFriendshipOfferRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                FriendshipOfferStatus.ACCEPTED, CREATED_AT, UPDATED_AT
        );

        var row = mapper.toRow(request);

        assertThat(row).isEqualTo(new FriendshipOfferRow(
                request.id(), request.fromUserId(), request.toUserId(),
                "ACCEPTED", CREATED_AT, UPDATED_AT
        ));
    }

    @Test
    @DisplayName("toResponse: строковый статус из Row преобразуется в enum")
    void whenToResponseThenStatusParsedToEnum() {
        var row = new FriendshipOfferRow(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "REJECTED", CREATED_AT, UPDATED_AT
        );

        var response = mapper.toResponse(row);

        assertThat(response).isEqualTo(new CreateFriendshipOfferResponse(
                row.id(), row.fromUserId(), row.toUserId(),
                FriendshipOfferStatus.REJECTED, CREATED_AT, UPDATED_AT
        ));
    }

    @Test
    @DisplayName("toResponse: неизвестный статус из БД приводит к IllegalArgumentException")
    void whenToResponseWithUnknownStatusThenThrowException() {
        var row = new FriendshipOfferRow(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "UNKNOWN", CREATED_AT, UPDATED_AT
        );

        assertThatThrownBy(() -> mapper.toResponse(row))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("toFindResponse: строковый статус из Row преобразуется в enum")
    void whenToFindResponseThenStatusParsedToEnum() {
        var row = new FriendshipOfferRow(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "ACCEPTED", CREATED_AT, UPDATED_AT
        );

        var response = mapper.toFindResponse(row);

        assertThat(response).isEqualTo(new FindFriendshipOfferResponse(
                row.id(), row.fromUserId(), row.toUserId(),
                FriendshipOfferStatus.ACCEPTED, CREATED_AT, UPDATED_AT
        ));
    }

    @Test
    @DisplayName("toFindResponse: неизвестный статус из БД приводит к IllegalArgumentException")
    void whenToFindResponseWithUnknownStatusThenThrowException() {
        var row = new FriendshipOfferRow(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "UNKNOWN", CREATED_AT, UPDATED_AT
        );

        assertThatThrownBy(() -> mapper.toFindResponse(row))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
