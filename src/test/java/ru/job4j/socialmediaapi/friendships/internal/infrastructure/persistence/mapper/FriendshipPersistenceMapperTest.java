package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.row.FriendshipsRow;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipsRepository.CreateFriendshipRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipsRepository.CreateFriendshipResponse;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Маппер дружбы: преобразование между Request/Response порта и FriendshipsRow")
class FriendshipPersistenceMapperTest {

    /** Fixed creation time of the friendship. */
    private static final Instant CREATED_AT = Instant.parse("2026-10-06T10:15:00Z");

    /** Mapper under test, created without Spring. */
    private final FriendshipPersistenceMapper mapper =
            Mappers.getMapper(FriendshipPersistenceMapper.class);

    @Test
    @DisplayName("toRow: статус-enum записывается в Row строкой через name()")
    void whenToRowThenStatusStoredAsName() {
        var request = new CreateFriendshipRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                FriendshipStatus.ACTIVE, CREATED_AT
        );

        var row = mapper.toRow(request);

        assertThat(row).isEqualTo(new FriendshipsRow(
                request.id(), request.firstUserId(), request.secondUserId(),
                request.friendshipOfferId(), "ACTIVE", CREATED_AT
        ));
    }

    @Test
    @DisplayName("toResponse: строковый статус из Row преобразуется в enum")
    void whenToResponseThenStatusParsedToEnum() {
        var row = new FriendshipsRow(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "DELETED", CREATED_AT
        );

        var response = mapper.toResponse(row);

        assertThat(response).isEqualTo(new CreateFriendshipResponse(
                row.id(), row.firstUserId(), row.secondUserId(),
                row.friendshipOfferId(), FriendshipStatus.DELETED, CREATED_AT
        ));
    }

    @Test
    @DisplayName("toResponse: неизвестный статус из БД выбросит IllegalArgumentException")
    void whenToResponseWithUnknownStatusThenThrowException() {
        var row = new FriendshipsRow(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "UNKNOWN", CREATED_AT
        );

        assertThatThrownBy(() -> mapper.toResponse(row))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
