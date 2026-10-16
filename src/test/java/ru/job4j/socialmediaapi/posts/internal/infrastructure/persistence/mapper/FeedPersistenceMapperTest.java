package ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.row.PostRow;
import ru.job4j.socialmediaapi.posts.internal.application.port.FeedRepository.FeedPostResponse;
import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Маппер ленты: преобразование PostRow в Response порта ленты")
class FeedPersistenceMapperTest {

    /** Fixed creation time of the post. */
    private static final Instant CREATED_AT = Instant.parse("2026-10-15T10:15:00Z");

    /** Fixed update time of the post. */
    private static final Instant UPDATED_AT = Instant.parse("2026-10-15T11:20:00Z");

    /** Mapper under test, created without Spring. */
    private final FeedPersistenceMapper mapper = Mappers.getMapper(FeedPersistenceMapper.class);

    @Test
    @DisplayName("toResponse: строковый статус из Row преобразуется в enum, остальные поля переносятся")
    void whenToResponseThenStatusParsedToEnum() {
        var row = new PostRow(
                UUID.randomUUID(), UUID.randomUUID(), "Заголовок", "Текст",
                "PUBLISHED", CREATED_AT, UPDATED_AT
        );

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
}
