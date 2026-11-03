package ru.job4j.socialmediaapi.posts.internal.application.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.job4j.socialmediaapi.posts.internal.application.model.FeedCursorResult;
import ru.job4j.socialmediaapi.posts.internal.application.model.PostResult;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedCursor;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedPageResponse;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedPostResponse;
import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PostApplicationMapper: страница ленты порта → результат сценария")
class PostApplicationMapperTest {

    /** Mapper under test. */
    private final PostApplicationMapper mapper = Mappers.getMapper(PostApplicationMapper.class);

    @Test
    @DisplayName("Посты и курсор next переносятся без изменений")
    void whenPageWithNextThenPostsAndCursor() {
        var id = UUID.randomUUID();
        var author = UUID.randomUUID();
        var time = Instant.parse("2026-10-15T10:00:00.123456Z");
        var page = new FeedPageResponse(
                List.of(new FeedPostResponse(id, author, "T", "C", PostStatus.PUBLISHED, time, time)),
                Optional.of(new FeedCursor(time, id))
        );

        var result = mapper.toResult(page);

        assertThat(result.posts()).containsExactly(
                new PostResult(id, author, "T", "C", PostStatus.PUBLISHED, time, time));
        assertThat(result.next()).isEqualTo(new FeedCursorResult(time, id));
    }

    @Test
    @DisplayName("Пустой next становится null")
    void whenNoNextThenNull() {
        var result = mapper.toResult(new FeedPageResponse(List.of(), Optional.empty()));

        assertThat(result.posts()).isEmpty();
        assertThat(result.next()).isNull();
    }
}
