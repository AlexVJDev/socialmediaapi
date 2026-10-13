package ru.job4j.socialmediaapi.adapter.out.persistence.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.job4j.socialmediaapi.adapter.out.persistence.entity.OfferFriendshipEntity;
import ru.job4j.socialmediaapi.application.port.out.OfferFriendshipRepository.CreateOfferFriendshipRequest;
import ru.job4j.socialmediaapi.application.port.out.OfferFriendshipRepository.CreateOfferFriendshipResponse;
import ru.job4j.socialmediaapi.application.port.out.OfferFriendshipRepository.FindOfferFriendshipResponse;
import ru.job4j.socialmediaapi.domain.offerfriendship.OfferFriendshipStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Маппер заявок в друзья: преобразование между Request/Response порта и OfferFriendshipEntity")
class OfferFriendshipPersistenceMapperTest {

    /** Fixed creation time of the offer. */
    private static final Instant CREATED_AT = Instant.parse("2026-10-06T10:15:00Z");

    /** Fixed update time of the offer. */
    private static final Instant UPDATED_AT = Instant.parse("2026-10-07T11:20:00Z");

    /** Mapper under test, created without Spring. */
    private final OfferFriendshipPersistenceMapper mapper =
            Mappers.getMapper(OfferFriendshipPersistenceMapper.class);

    @Test
    @DisplayName("toEntity: статус-enum записывается в Entity строкой через name()")
    void whenToEntityThenStatusStoredAsName() {
        var request = new CreateOfferFriendshipRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                OfferFriendshipStatus.ACCEPTED, CREATED_AT, UPDATED_AT
        );

        var entity = mapper.toEntity(request);

        assertThat(entity).isEqualTo(new OfferFriendshipEntity(
                request.id(), request.fromUserId(), request.toUserId(),
                "ACCEPTED", CREATED_AT, UPDATED_AT
        ));
    }

    @Test
    @DisplayName("toResponse: строковый статус из Entity преобразуется в enum")
    void whenToResponseThenStatusParsedToEnum() {
        var entity = new OfferFriendshipEntity(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "REJECTED", CREATED_AT, UPDATED_AT
        );

        var response = mapper.toResponse(entity);

        assertThat(response).isEqualTo(new CreateOfferFriendshipResponse(
                entity.id(), entity.fromUserId(), entity.toUserId(),
                OfferFriendshipStatus.REJECTED, CREATED_AT, UPDATED_AT
        ));
    }

    @Test
    @DisplayName("toResponse: неизвестный статус из БД приводит к IllegalArgumentException")
    void whenToResponseWithUnknownStatusThenThrowException() {
        var entity = new OfferFriendshipEntity(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "UNKNOWN", CREATED_AT, UPDATED_AT
        );

        assertThatThrownBy(() -> mapper.toResponse(entity))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("toFindResponse: строковый статус из Entity преобразуется в enum")
    void whenToFindResponseThenStatusParsedToEnum() {
        var entity = new OfferFriendshipEntity(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "ACCEPTED", CREATED_AT, UPDATED_AT
        );

        var response = mapper.toFindResponse(entity);

        assertThat(response).isEqualTo(new FindOfferFriendshipResponse(
                entity.id(), entity.fromUserId(), entity.toUserId(),
                OfferFriendshipStatus.ACCEPTED, CREATED_AT, UPDATED_AT
        ));
    }

    @Test
    @DisplayName("toFindResponse: неизвестный статус из БД приводит к IllegalArgumentException")
    void whenToFindResponseWithUnknownStatusThenThrowException() {
        var entity = new OfferFriendshipEntity(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "UNKNOWN", CREATED_AT, UPDATED_AT
        );

        assertThatThrownBy(() -> mapper.toFindResponse(entity))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
