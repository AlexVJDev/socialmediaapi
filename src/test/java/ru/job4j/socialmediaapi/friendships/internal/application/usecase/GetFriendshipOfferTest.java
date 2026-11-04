package ru.job4j.socialmediaapi.friendships.internal.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.job4j.socialmediaapi.friendships.internal.application.mapper.FriendshipOfferApplicationMapper;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.GetFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipOfferRepository;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FindFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipRequestNotFoundException;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Сценарий чтения заявки в друзья по id")
class GetFriendshipOfferTest {

    /** Fixed creation time of the offer. */
    private static final Instant CREATED_AT = Instant.parse("2026-10-12T10:15:00Z");

    /** Fixed decision time of the offer. */
    private static final Instant UPDATED_AT = Instant.parse("2026-10-12T10:20:00Z");

    /** Mocked offer repository. */
    @Mock
    private FriendshipOfferRepository repository;

    /** Use case under test. */
    private GetFriendshipOffer useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetFriendshipOffer(repository, Mappers.getMapper(FriendshipOfferApplicationMapper.class));
    }

    @Test
    @DisplayName("Заявка есть: все поля переносятся в результат")
    void whenOfferExistsThenResult() {
        var stored = new FindFriendshipOfferResponse(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                FriendshipOfferStatus.REJECTED, CREATED_AT, UPDATED_AT
        );
        when(repository.findById(stored.id())).thenReturn(Optional.of(stored));

        var result = useCase.get(new GetFriendshipOfferCommand(stored.id()));

        assertThat(result).isEqualTo(new FriendshipOfferResult(
                stored.id(), stored.fromUserId(), stored.toUserId(),
                FriendshipOfferStatus.REJECTED, CREATED_AT, UPDATED_AT
        ));
    }

    @Test
    @DisplayName("Заявки нет: FriendshipRequestNotFoundException")
    void whenNoOfferThenNotFound() {
        var offerId = UUID.randomUUID();
        when(repository.findById(offerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.get(new GetFriendshipOfferCommand(offerId)))
                .isInstanceOf(FriendshipRequestNotFoundException.class)
                .hasMessageContaining(offerId.toString());
    }
}
