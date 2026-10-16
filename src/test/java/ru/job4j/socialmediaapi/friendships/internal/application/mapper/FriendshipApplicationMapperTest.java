package ru.job4j.socialmediaapi.friendships.internal.application.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipRequest;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.Friendship;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Прикладной маппер дружбы: доменная дружба → запрос порта")
class FriendshipApplicationMapperTest {

    /** Mapper under test, created without Spring. */
    private final FriendshipApplicationMapper mapper = Mappers.getMapper(FriendshipApplicationMapper.class);

    @Test
    @DisplayName("toRequest: пара в каноническом порядке, заявка, статус и время переносятся без изменений")
    void whenToRequestThenAllFieldsCopied() {
        var friendship = Friendship.between(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.parse("2026-10-06T10:20:00Z")
        );

        var request = mapper.toRequest(friendship);

        assertThat(request).isEqualTo(new CreateFriendshipRequest(
                friendship.id(), friendship.firstUserId(), friendship.secondUserId(),
                friendship.friendshipOfferId(), friendship.status(), friendship.createdAt()
        ));
    }
}
