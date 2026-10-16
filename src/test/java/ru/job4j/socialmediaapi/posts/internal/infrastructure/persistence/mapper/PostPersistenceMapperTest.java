package ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.row.PostRow;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.CreatePostRequest;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.PostResponse;
import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Маппер публикаций: преобразование между Request/Response порта и PostRow")
class PostPersistenceMapperTest {

    /** Fixed creation time of the post. */
    private static final Instant CREATED_AT = Instant.parse("2026-10-15T10:15:00Z");

    /** Fixed update time of the post. */
    private static final Instant UPDATED_AT = Instant.parse("2026-10-15T11:20:00Z");

    /** Mapper under test, created without Spring. */
    private final PostPersistenceMapper mapper = Mappers.getMapper(PostPersistenceMapper.class);

    @Test
    @DisplayName("toRow: статус-enum записывается в Row строкой через name(), остальные поля переносятся")
    void whenToRowThenStatusStoredAsName() {
        var request = new CreatePostRequest(
                UUID.randomUUID(), UUID.randomUUID(), "Заголовок", "Текст",
                PostStatus.PUBLISHED, CREATED_AT, UPDATED_AT
        );

        var row = mapper.toRow(request);

        assertThat(row).isEqualTo(new PostRow(
                request.id(), request.authorId(), "Заголовок", "Текст",
                "PUBLISHED", CREATED_AT, UPDATED_AT
        ));
    }

    @Test
    @DisplayName("toResponse: строковый статус из Row преобразуется в enum")
    void whenToResponseThenStatusParsedToEnum() {
        var row = new PostRow(
                UUID.randomUUID(), UUID.randomUUID(), "Заголовок", "Текст",
                "DELETED", CREATED_AT, UPDATED_AT
        );

        var response = mapper.toResponse(row);

        assertThat(response).isEqualTo(new PostResponse(
                row.id(), row.authorId(), "Заголовок", "Текст",
                PostStatus.DELETED, CREATED_AT, UPDATED_AT
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
