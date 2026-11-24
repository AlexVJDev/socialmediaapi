package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FindFriendResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendCursor;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipStatus;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.jpa.FriendView;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.jpa.FriendshipEntityFixtures.friendship;

@DisplayName("Маппер дружбы: FriendshipEntity и FriendView → модели порта")
class FriendshipPersistenceMapperTest {

    /** Фиксированное время создания дружбы. */
    private static final Instant CREATED_AT = Instant.parse("2026-10-06T10:15:00Z");

    /** Проверяемый маппер, создан без Spring. */
    private final FriendshipPersistenceMapper mapper =
            Mappers.getMapper(FriendshipPersistenceMapper.class);

    @Test
    @DisplayName("toResponse: все поля сущности переносятся в ответ на создание дружбы")
    void whenToResponseThenAllFieldsCopied() {
        var entity = friendship(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                FriendshipStatus.ACTIVE, CREATED_AT
        );

        assertThat(mapper.toResponse(entity)).isEqualTo(new CreateFriendshipResponse(
                entity.getId(), entity.getFirstUserId(), entity.getSecondUserId(),
                entity.getFriendshipOfferId(), FriendshipStatus.ACTIVE, CREATED_AT
        ));
    }

    @Test
    @DisplayName("toFindResponse: друг — второй участник дружбы (friendId → userId)")
    void whenToFindResponseThenFriendIdBecomesUserId() {
        var view = new View(UUID.randomUUID(), UUID.randomUUID(), CREATED_AT);

        assertThat(mapper.toFindResponse(view)).isEqualTo(new FindFriendResponse(view.getId(), view.getFriendId()));
    }

    @Test
    @DisplayName("toCursor: курсор — created_at и id дружбы")
    void whenToCursorThenCreatedAtAndId() {
        var view = new View(UUID.randomUUID(), UUID.randomUUID(), CREATED_AT);

        assertThat(mapper.toCursor(view)).isEqualTo(new FriendCursor(CREATED_AT, view.getId()));
    }

    /**
     * Строка страницы друзей без базы данных.
     *
     * @param getId        id дружбы
     * @param getFriendId  второй участник
     * @param getCreatedAt время создания
     */
    private record View(UUID getId, UUID getFriendId, Instant getCreatedAt) implements FriendView {
    }
}
