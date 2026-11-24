package ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FindFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendshipOfferCursor;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.UpdateFriendshipOfferStatusResponse;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.jpa.FriendshipOfferEntity;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.job4j.socialmediaapi.friendships.internal.infrastructure.persistence.jpa.FriendshipOfferEntityFixtures.offer;

@DisplayName("Маппер заявок в друзья: FriendshipOfferEntity → модели порта")
class FriendshipOfferPersistenceMapperTest {

    /** Фиксированное время создания заявки. */
    private static final Instant CREATED_AT = Instant.parse("2026-10-06T10:15:00Z");

    /** Фиксированное время изменения заявки. */
    private static final Instant UPDATED_AT = Instant.parse("2026-10-07T11:20:00Z");

    /** Проверяемый маппер, создан без Spring. */
    private final FriendshipOfferPersistenceMapper mapper =
            Mappers.getMapper(FriendshipOfferPersistenceMapper.class);

    /**
     * @param status статус заявки
     * @return заявка в этом статусе со случайными id и фиксированным временем
     */
    private static FriendshipOfferEntity entity(FriendshipOfferStatus status) {
        return offer(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), status, CREATED_AT, UPDATED_AT);
    }

    @Test
    @DisplayName("toResponse: все поля сущности переносятся в ответ на создание")
    void whenToResponseThenAllFieldsCopied() {
        var entity = entity(FriendshipOfferStatus.PENDING);

        assertThat(mapper.toResponse(entity)).isEqualTo(new CreateFriendshipOfferResponse(
                entity.getId(), entity.getFromUserId(), entity.getToUserId(),
                FriendshipOfferStatus.PENDING, CREATED_AT, UPDATED_AT
        ));
    }

    @Test
    @DisplayName("toFindResponse: все поля сущности переносятся в ответ на поиск")
    void whenToFindResponseThenAllFieldsCopied() {
        var entity = entity(FriendshipOfferStatus.ACCEPTED);

        assertThat(mapper.toFindResponse(entity)).isEqualTo(new FindFriendshipOfferResponse(
                entity.getId(), entity.getFromUserId(), entity.getToUserId(),
                FriendshipOfferStatus.ACCEPTED, CREATED_AT, UPDATED_AT
        ));
    }

    @Test
    @DisplayName("toUpdateResponse: все поля сущности переносятся в ответ на смену статуса")
    void whenToUpdateResponseThenAllFieldsCopied() {
        var entity = entity(FriendshipOfferStatus.REJECTED);

        assertThat(mapper.toUpdateResponse(entity)).isEqualTo(new UpdateFriendshipOfferStatusResponse(
                entity.getId(), entity.getFromUserId(), entity.getToUserId(),
                FriendshipOfferStatus.REJECTED, CREATED_AT, UPDATED_AT
        ));
    }

    @Test
    @DisplayName("toCursor: курсор — created_at и id заявки")
    void whenToCursorThenCreatedAtAndId() {
        var entity = entity(FriendshipOfferStatus.PENDING);

        assertThat(mapper.toCursor(entity)).isEqualTo(new FriendshipOfferCursor(CREATED_AT, entity.getId()));
    }
}
