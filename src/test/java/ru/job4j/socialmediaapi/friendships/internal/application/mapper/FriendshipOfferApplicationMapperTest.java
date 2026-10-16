package ru.job4j.socialmediaapi.friendships.internal.application.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FindFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferToSelfException;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOffer;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Прикладной маппер заявок: ответ порта → доменная заявка")
class FriendshipOfferApplicationMapperTest {

    /** Fixed creation time. */
    private static final Instant CREATED_AT = Instant.parse("2026-10-06T10:15:00Z");

    /** Fixed update time. */
    private static final Instant UPDATED_AT = Instant.parse("2026-10-06T10:20:00Z");

    /** Mapper under test, created without Spring. */
    private final FriendshipOfferApplicationMapper mapper =
            Mappers.getMapper(FriendshipOfferApplicationMapper.class);

    @Test
    @DisplayName("toDomain: все поля переносятся через конструктор записи")
    void whenToDomainThenAllFieldsCopied() {
        var response = new FindFriendshipOfferResponse(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                FriendshipOfferStatus.ACCEPTED, CREATED_AT, UPDATED_AT
        );

        var offer = mapper.toDomain(response);

        assertThat(offer).isEqualTo(new FriendshipOffer(
                response.id(), response.fromUserId(), response.toUserId(),
                FriendshipOfferStatus.ACCEPTED, CREATED_AT, UPDATED_AT
        ));
    }

    @Test
    @DisplayName("toDomain: инварианты домена проверяются снова — заявка самому себе выбросит исключение")
    void whenToDomainBreaksInvariantThenThrowException() {
        var userId = UUID.randomUUID();
        var response = new FindFriendshipOfferResponse(
                UUID.randomUUID(), userId, userId, FriendshipOfferStatus.PENDING, CREATED_AT, UPDATED_AT
        );

        assertThatThrownBy(() -> mapper.toDomain(response))
                .isInstanceOf(FriendshipOfferToSelfException.class);
    }
}
