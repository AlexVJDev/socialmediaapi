package ru.job4j.socialmediaapi.posts.internal.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.job4j.socialmediaapi.posts.internal.application.exception.IncompleteFeedCursorException;
import ru.job4j.socialmediaapi.posts.internal.application.mapper.PostApplicationMapper;
import ru.job4j.socialmediaapi.posts.internal.application.model.GetFeedCommand;
import ru.job4j.socialmediaapi.posts.internal.application.port.FeedRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedCursor;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedPageResponse;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Сценарий чтения ленты: первая страница без курсора, следующие — после курсора")
class GetFeedTest {

    /** Reader of the feed. */
    private static final UUID READER = UUID.randomUUID();

    /** Empty page returned by the mocked repository. */
    private static final FeedPageResponse EMPTY = new FeedPageResponse(List.of(), Optional.empty());

    /** Mocked feed repository. */
    @Mock
    private FeedRepository feedRepository;

    /** Use case under test. */
    private GetFeed useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetFeed(feedRepository, Mappers.getMapper(PostApplicationMapper.class));
    }

    @Test
    @DisplayName("Без курсора читается первая страница")
    void whenNoCursorThenFirstPage() {
        when(feedRepository.findFeed(READER, 10)).thenReturn(EMPTY);

        var result = useCase.get(new GetFeedCommand(READER, null, null, 10));

        assertThat(result.posts()).isEmpty();
        assertThat(result.next()).isNull();
    }

    @Test
    @DisplayName("С курсором читаются посты после него")
    void whenCursorThenPageAfter() {
        var cursor = new FeedCursor(Instant.parse("2026-10-15T10:00:00.123456Z"), UUID.randomUUID());
        when(feedRepository.findFeedAfter(READER, cursor, 10)).thenReturn(EMPTY);

        useCase.get(new GetFeedCommand(READER, cursor.createdAt(), cursor.id(), 10));

        verify(feedRepository).findFeedAfter(READER, cursor, 10);
    }

    @Test
    @DisplayName("Курсор задан наполовину: IncompleteFeedCursorException, лента не читается")
    void whenCursorPartialThenIncompleteCursor() {
        assertThatThrownBy(() -> useCase.get(new GetFeedCommand(READER, null, UUID.randomUUID(), 10)))
                .isInstanceOf(IncompleteFeedCursorException.class);
        assertThatThrownBy(() -> useCase.get(new GetFeedCommand(READER, Instant.EPOCH, null, 10)))
                .isInstanceOf(IncompleteFeedCursorException.class);
        verifyNoInteractions(feedRepository);
    }

    @Test
    @DisplayName("Ошибка лимита из репозитория пробрасывается как есть")
    void whenLimitRejectedThenIllegalArgument() {
        when(feedRepository.findFeed(any(), anyInt())).thenThrow(new IllegalArgumentException("limit"));

        assertThatThrownBy(() -> useCase.get(new GetFeedCommand(READER, null, null, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
