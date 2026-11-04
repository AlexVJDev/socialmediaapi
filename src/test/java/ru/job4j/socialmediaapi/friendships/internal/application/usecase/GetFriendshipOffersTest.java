package ru.job4j.socialmediaapi.friendships.internal.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.job4j.socialmediaapi.friendships.internal.application.exception.IncompleteFriendshipOfferCursorException;
import ru.job4j.socialmediaapi.friendships.internal.application.mapper.FriendshipOfferApplicationMapper;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferCursorResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.GetFriendshipOffersCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipOfferRepository;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FindFriendshipOfferResponse;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendshipOfferCursor;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendshipOfferPageResponse;
import ru.job4j.socialmediaapi.friendships.internal.domain.model.FriendshipOfferStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Сценарий чтения страницы всех заявок: первая страница без курсора, следующие — после курсора")
class GetFriendshipOffersTest {

    /** Fixed creation time of the offer. */
    private static final Instant CREATED_AT = Instant.parse("2026-10-12T10:15:00Z");

    /** Empty page returned by the mocked repository. */
    private static final FriendshipOfferPageResponse EMPTY =
            new FriendshipOfferPageResponse(List.of(), Optional.empty());

    /** Mocked offer repository. */
    @Mock
    private FriendshipOfferRepository repository;

    /** Use case under test. */
    private GetFriendshipOffers useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetFriendshipOffers(repository, Mappers.getMapper(FriendshipOfferApplicationMapper.class));
    }

    @Test
    @DisplayName("Без курсора читается первая страница; заявки и next переносятся в результат")
    void whenNoCursorThenFirstPage() {
        var offer = new FindFriendshipOfferResponse(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                FriendshipOfferStatus.PENDING, CREATED_AT, CREATED_AT
        );
        when(repository.findPage(1)).thenReturn(new FriendshipOfferPageResponse(
                List.of(offer), Optional.of(new FriendshipOfferCursor(CREATED_AT, offer.id()))
        ));

        var result = useCase.get(new GetFriendshipOffersCommand(null, null, 1));

        assertThat(result.offers()).containsExactly(new FriendshipOfferResult(
                offer.id(), offer.fromUserId(), offer.toUserId(),
                FriendshipOfferStatus.PENDING, CREATED_AT, CREATED_AT
        ));
        assertThat(result.next()).isEqualTo(new FriendshipOfferCursorResult(CREATED_AT, offer.id()));
    }

    @Test
    @DisplayName("С курсором читаются заявки после него; пустой next становится null")
    void whenCursorThenPageAfter() {
        var afterId = UUID.randomUUID();
        when(repository.findPageAfter(new FriendshipOfferCursor(CREATED_AT, afterId), 20)).thenReturn(EMPTY);

        var result = useCase.get(new GetFriendshipOffersCommand(CREATED_AT, afterId, 20));

        assertThat(result.offers()).isEmpty();
        assertThat(result.next()).isNull();
    }

    @Test
    @DisplayName("Задана только одна часть курсора: IncompleteFriendshipOfferCursorException, хранилище не читается")
    void whenCursorIncompleteThenException() {
        assertThatThrownBy(() -> useCase.get(new GetFriendshipOffersCommand(CREATED_AT, null, 20)))
                .isInstanceOf(IncompleteFriendshipOfferCursorException.class);
        assertThatThrownBy(() -> useCase.get(new GetFriendshipOffersCommand(null, UUID.randomUUID(), 20)))
                .isInstanceOf(IncompleteFriendshipOfferCursorException.class);
        verifyNoInteractions(repository);
    }
}
