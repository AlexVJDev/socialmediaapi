package ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedCursor;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedPageResponse;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedPostResponse;
import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.row.PostRow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Маппер ленты: преобразование PostRow в Response и курсор порта ленты")
class FeedPersistenceMapperTest {

    /** Fixed creation time of the post. */
    private static final Instant CREATED_AT = Instant.parse("2026-10-15T10:15:00.123456Z");

    /** Fixed update time of the post. */
    private static final Instant UPDATED_AT = Instant.parse("2026-10-15T11:20:00Z");

    /** Mapper under test, created without Spring. */
    private final FeedPersistenceMapper mapper = Mappers.getMapper(FeedPersistenceMapper.class);

    @Test
    @DisplayName("toResponse: строковый статус из Row преобразуется в enum, остальные поля переносятся")
    void whenToResponseThenStatusParsedToEnum() {
        var row = publishedRow();

        var response = mapper.toResponse(row);

        assertThat(response).isEqualTo(new FeedPostResponse(
                row.id(), row.authorId(), "Заголовок", "Текст",
                PostStatus.PUBLISHED, CREATED_AT, UPDATED_AT
        ));
    }

    @Test
    @DisplayName("toResponse: неизвестный статус из БД приводит к IllegalArgumentException")
    void whenToResponseWithUnknownStatusThenThrowException() {
        var row = new PostRow(
                UUID.randomUUID(), UUID.randomUUID(), "Заголовок", "Текст",
                "UNKNOWN", CREATED_AT, UPDATED_AT
        );

        assertThatThrownBy(() -> mapper.toResponse(row))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("toCursor: createdAt и id переносятся из Row без изменений")
    void whenToCursorThenCreatedAtAndIdCopied() {
        var row = publishedRow();

        var cursor = mapper.toCursor(row);

        assertThat(cursor).isEqualTo(new FeedCursor(CREATED_AT, row.id()));
    }

    @Test
    @DisplayName("FeedCursor: null в createdAt или id отклоняется NullPointerException")
    void whenCursorWithNullThenThrowException() {
        assertThatThrownBy(() -> new FeedCursor(null, UUID.randomUUID()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("createdAt");
        assertThatThrownBy(() -> new FeedCursor(CREATED_AT, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("id");
    }

    @Test
    @DisplayName("FeedPageResponse: список постов копируется и не изменяется снаружи")
    void whenPageCreatedThenPostsCopiedAndUnmodifiable() {
        var posts = new ArrayList<FeedPostResponse>();
        posts.add(mapper.toResponse(publishedRow()));
        var page = new FeedPageResponse(posts, Optional.empty());

        posts.clear();

        assertThat(page.posts()).hasSize(1);
        assertThatThrownBy(() -> page.posts().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("FeedPageResponse: null вместо next отклоняется NullPointerException")
    void whenPageWithNullNextThenThrowException() {
        assertThatThrownBy(() -> new FeedPageResponse(List.of(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("next");
    }

    private static PostRow publishedRow() {
        return new PostRow(
                UUID.randomUUID(), UUID.randomUUID(), "Заголовок", "Текст",
                "PUBLISHED", CREATED_AT, UPDATED_AT
        );
    }
}
