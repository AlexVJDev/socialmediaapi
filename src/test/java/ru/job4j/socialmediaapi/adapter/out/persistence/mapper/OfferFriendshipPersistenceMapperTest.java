package ru.job4j.socialmediaapi.adapter.out.persistence.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.job4j.socialmediaapi.adapter.out.persistence.entity.OfferFriendshipEntity;
import ru.job4j.socialmediaapi.application.port.out.OfferFriendshipRepository.CreateOfferFriendshipRequest;
import ru.job4j.socialmediaapi.application.port.out.OfferFriendshipRepository.CreateOfferFriendshipResponse;
import ru.job4j.socialmediaapi.domain.offerfriendship.OfferFriendshipStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OfferFriendshipPersistenceMapperTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-06T10:15:00Z");

    private static final Instant UPDATED_AT = Instant.parse("2026-10-07T11:20:00Z");

    private final OfferFriendshipPersistenceMapper mapper =
            Mappers.getMapper(OfferFriendshipPersistenceMapper.class);

    @Test
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
    void whenToResponseWithUnknownStatusThenThrowException() {
        var entity = new OfferFriendshipEntity(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "UNKNOWN", CREATED_AT, UPDATED_AT
        );

        assertThatThrownBy(() -> mapper.toResponse(entity))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
