package ru.job4j.socialmediaapi.posts.internal.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.job4j.socialmediaapi.posts.internal.application.mapper.PostApplicationMapper;
import ru.job4j.socialmediaapi.posts.internal.application.model.GetPostCommand;
import ru.job4j.socialmediaapi.posts.internal.application.model.PostResult;
import ru.job4j.socialmediaapi.posts.internal.application.port.PostRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.PostResponse;
import ru.job4j.socialmediaapi.posts.internal.domain.exception.PostNotFoundException;
import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Сценарий чтения поста по id")
class GetPostTest {

    /** Post to read. */
    private static final UUID POST_ID = UUID.randomUUID();

    /** Mocked post repository. */
    @Mock
    private PostRepository postRepository;

    /** Use case under test. */
    private GetPost useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetPost(postRepository, Mappers.getMapper(PostApplicationMapper.class));
    }

    @Test
    @DisplayName("Опубликованный пост возвращается со всеми полями")
    void whenPublishedThenResult() {
        var author = UUID.randomUUID();
        var time = Instant.parse("2026-10-15T10:00:00Z");
        when(postRepository.findPublishedById(POST_ID)).thenReturn(Optional.of(new PostResponse(
                POST_ID, author, "Заголовок", "Текст", PostStatus.PUBLISHED, time, time)));

        assertThat(useCase.get(new GetPostCommand(POST_ID))).isEqualTo(new PostResult(
                POST_ID, author, "Заголовок", "Текст", PostStatus.PUBLISHED, time, time));
    }

    @Test
    @DisplayName("Нет поста или он удалён: PostNotFoundException")
    void whenMissingThenNotFound() {
        when(postRepository.findPublishedById(POST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.get(new GetPostCommand(POST_ID)))
                .isInstanceOf(PostNotFoundException.class)
                .hasMessageContaining(POST_ID.toString());
    }
}
