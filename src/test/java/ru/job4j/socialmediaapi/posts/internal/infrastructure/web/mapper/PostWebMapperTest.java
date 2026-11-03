package ru.job4j.socialmediaapi.posts.internal.infrastructure.web.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.job4j.socialmediaapi.posts.internal.application.model.FeedCursorResult;
import ru.job4j.socialmediaapi.posts.internal.application.model.FeedPageResult;
import ru.job4j.socialmediaapi.posts.internal.application.model.GetFeedCommand;
import ru.job4j.socialmediaapi.posts.internal.application.model.PostResult;
import ru.job4j.socialmediaapi.posts.internal.application.usecase.GetFeed;
import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.web.request.FeedWebRequest;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.web.response.FeedCursorWebResponse;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.web.response.PostWebResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PostWebMapper: параметры ленты → команда, результат → ответ")
class PostWebMapperTest {

    /** Mapper under test. */
    private final PostWebMapper mapper = Mappers.getMapper(PostWebMapper.class);

    @Test
    @DisplayName("Без limit берётся размер страницы по умолчанию")
    void whenLimitMissingThenDefault() {
        var reader = UUID.randomUUID();

        assertThat(mapper.toGetFeedCommand(new FeedWebRequest(reader, null, null, null)))
                .isEqualTo(new GetFeedCommand(reader, null, null, GetFeed.DEFAULT_LIMIT));
    }

    @Test
    @DisplayName("Статус — строка, курсор переносится, отсутствующий next остаётся null")
    void whenPageThenResponse() {
        var id = UUID.randomUUID();
        var author = UUID.randomUUID();
        var time = Instant.parse("2026-10-15T10:00:00Z");
        var post = new PostResult(id, author, "T", "C", PostStatus.PUBLISHED, time, time);

        var withNext = mapper.toResponse(new FeedPageResult(List.of(post), new FeedCursorResult(time, id)));
        var last = mapper.toResponse(new FeedPageResult(List.of(), null));

        assertThat(withNext.posts()).containsExactly(
                new PostWebResponse(id, author, "T", "C", "PUBLISHED", time, time));
        assertThat(withNext.next()).isEqualTo(new FeedCursorWebResponse(time, id));
        assertThat(last.next()).isNull();
    }
}
