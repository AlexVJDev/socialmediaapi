package ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedCursor;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedPostResponse;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.PostResponse;
import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.jpa.PostEntity;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.job4j.socialmediaapi.posts.internal.infrastructure.persistence.jpa.PostEntityFixtures.post;

@DisplayName("Маппер публикаций: PostEntity → модели порта")
class PostPersistenceMapperTest {

    /** Фиксированное время создания публикации. */
    private static final Instant CREATED_AT = Instant.parse("2026-10-15T10:15:00Z");

    /** Фиксированное время изменения публикации. */
    private static final Instant UPDATED_AT = Instant.parse("2026-10-15T11:20:00Z");

    /** Проверяемый маппер, создан без Spring. */
    private final PostPersistenceMapper mapper = Mappers.getMapper(PostPersistenceMapper.class);

    /**
     * @param status статус публикации
     * @return публикация в этом статусе со случайными id и фиксированным временем
     */
    private static PostEntity entity(PostStatus status) {
        return post(UUID.randomUUID(), UUID.randomUUID(), "Заголовок", "Текст", status, CREATED_AT, UPDATED_AT);
    }

    @Test
    @DisplayName("toResponse: все поля сущности переносятся в публикацию порта")
    void whenToResponseThenAllFieldsCopied() {
        var entity = entity(PostStatus.DELETED);

        assertThat(mapper.toResponse(entity)).isEqualTo(new PostResponse(
                entity.getId(), entity.getAuthorId(), "Заголовок", "Текст",
                PostStatus.DELETED, CREATED_AT, UPDATED_AT
        ));
    }

    @Test
    @DisplayName("toFeedResponse: все поля сущности переносятся в пост страницы")
    void whenToFeedResponseThenAllFieldsCopied() {
        var entity = entity(PostStatus.PUBLISHED);

        assertThat(mapper.toFeedResponse(entity)).isEqualTo(new FeedPostResponse(
                entity.getId(), entity.getAuthorId(), "Заголовок", "Текст",
                PostStatus.PUBLISHED, CREATED_AT, UPDATED_AT
        ));
    }

    @Test
    @DisplayName("toCursor: курсор — created_at и id публикации")
    void whenToCursorThenCreatedAtAndId() {
        var entity = entity(PostStatus.PUBLISHED);

        assertThat(mapper.toCursor(entity)).isEqualTo(new FeedCursor(CREATED_AT, entity.getId()));
    }
}
