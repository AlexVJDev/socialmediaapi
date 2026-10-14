package ru.job4j.socialmediaapi.adapter.out.persistence.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.job4j.socialmediaapi.adapter.out.persistence.entity.FriendshipsEntity;
import ru.job4j.socialmediaapi.application.port.out.FriendshipsRepository.CreateFriendshipRequest;
import ru.job4j.socialmediaapi.application.port.out.FriendshipsRepository.CreateFriendshipResponse;
import ru.job4j.socialmediaapi.domain.friendship.FriendshipStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Маппер дружбы: преобразование между Request/Response порта и FriendshipEntity")
class FriendshipPersistenceMapperTest {

    /** Fixed creation time of the friendship. */
    private static final Instant CREATED_AT = Instant.parse("2026-10-06T10:15:00Z");

    /** Mapper under test, created without Spring. */
    private final FriendshipPersistenceMapper mapper =
            Mappers.getMapper(FriendshipPersistenceMapper.class);

    @Test
    @DisplayName("toEntity: статус-enum записывается в Entity строкой через name()")
    void whenToEntityThenStatusStoredAsName() {
        var request = new CreateFriendshipRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                FriendshipStatus.ACTIVE, CREATED_AT
        );

        var entity = mapper.toEntity(request);

        assertThat(entity).isEqualTo(new FriendshipsEntity(
                request.id(), request.firstUserId(), request.secondUserId(),
                request.friendshipOfferId(), "ACTIVE", CREATED_AT
        ));
    }

    @Test
    @DisplayName("toResponse: строковый статус из Entity преобразуется в enum")
    void whenToResponseThenStatusParsedToEnum() {
        var entity = new FriendshipsEntity(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "DELETED", CREATED_AT
        );

        var response = mapper.toResponse(entity);

        assertThat(response).isEqualTo(new CreateFriendshipResponse(
                entity.id(), entity.firstUserId(), entity.secondUserId(),
                entity.friendshipOfferId(), FriendshipStatus.DELETED, CREATED_AT
        ));
    }

    @Test
    @DisplayName("toResponse: неизвестный статус из БД выбросит IllegalArgumentException")
    void whenToResponseWithUnknownStatusThenThrowException() {
        var entity = new FriendshipsEntity(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "UNKNOWN", CREATED_AT
        );

        assertThatThrownBy(() -> mapper.toResponse(entity))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
