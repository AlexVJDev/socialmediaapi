package ru.job4j.socialmediaapi.adapter.out.persistence.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.job4j.socialmediaapi.adapter.out.persistence.entity.FriendshipOfferEntity;
import ru.job4j.socialmediaapi.application.port.out.FriendshipOfferRepository.CreateFriendshipOfferRequest;
import ru.job4j.socialmediaapi.application.port.out.FriendshipOfferRepository.CreateFriendshipOfferResponse;
import ru.job4j.socialmediaapi.application.port.out.FriendshipOfferRepository.FindFriendshipOfferResponse;
import ru.job4j.socialmediaapi.domain.friendshipoffer.FriendshipOfferStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Маппер заявок в друзья: преобразование между Request/Response порта и FriendshipOfferEntity")
class FriendshipOfferPersistenceMapperTest {

    /** Fixed creation time of the offer. */
    private static final Instant CREATED_AT = Instant.parse("2026-10-06T10:15:00Z");

    /** Fixed update time of the offer. */
    private static final Instant UPDATED_AT = Instant.parse("2026-10-07T11:20:00Z");

    /** Mapper under test, created without Spring. */
    private final FriendshipOfferPersistenceMapper mapper =
            Mappers.getMapper(FriendshipOfferPersistenceMapper.class);

    @Test
    @DisplayName("toEntity: статус-enum записывается в Entity строкой через name()")
    void whenToEntityThenStatusStoredAsName() {
        var request = new CreateFriendshipOfferRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                FriendshipOfferStatus.ACCEPTED, CREATED_AT, UPDATED_AT
        );

        var entity = mapper.toEntity(request);

        assertThat(entity).isEqualTo(new FriendshipOfferEntity(
                request.id(), request.fromUserId(), request.toUserId(),
                "ACCEPTED", CREATED_AT, UPDATED_AT
        ));
    }

    @Test
    @DisplayName("toResponse: строковый статус из Entity преобразуется в enum")
    void whenToResponseThenStatusParsedToEnum() {
        var entity = new FriendshipOfferEntity(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "REJECTED", CREATED_AT, UPDATED_AT
        );

        var response = mapper.toResponse(entity);

        assertThat(response).isEqualTo(new CreateFriendshipOfferResponse(
                entity.id(), entity.fromUserId(), entity.toUserId(),
                FriendshipOfferStatus.REJECTED, CREATED_AT, UPDATED_AT
        ));
    }

    @Test
    @DisplayName("toResponse: неизвестный статус из БД приводит к IllegalArgumentException")
    void whenToResponseWithUnknownStatusThenThrowException() {
        var entity = new FriendshipOfferEntity(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "UNKNOWN", CREATED_AT, UPDATED_AT
        );

        assertThatThrownBy(() -> mapper.toResponse(entity))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("toFindResponse: строковый статус из Entity преобразуется в enum")
    void whenToFindResponseThenStatusParsedToEnum() {
        var entity = new FriendshipOfferEntity(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "ACCEPTED", CREATED_AT, UPDATED_AT
        );

        var response = mapper.toFindResponse(entity);

        assertThat(response).isEqualTo(new FindFriendshipOfferResponse(
                entity.id(), entity.fromUserId(), entity.toUserId(),
                FriendshipOfferStatus.ACCEPTED, CREATED_AT, UPDATED_AT
        ));
    }

    @Test
    @DisplayName("toFindResponse: неизвестный статус из БД приводит к IllegalArgumentException")
    void whenToFindResponseWithUnknownStatusThenThrowException() {
        var entity = new FriendshipOfferEntity(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "UNKNOWN", CREATED_AT, UPDATED_AT
        );

        assertThatThrownBy(() -> mapper.toFindResponse(entity))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
