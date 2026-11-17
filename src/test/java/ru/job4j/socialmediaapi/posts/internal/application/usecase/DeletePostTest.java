package ru.job4j.socialmediaapi.posts.internal.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.job4j.socialmediaapi.posts.internal.application.mapper.PostApplicationMapper;
import ru.job4j.socialmediaapi.posts.internal.application.model.DeletePostCommand;
import ru.job4j.socialmediaapi.posts.internal.application.port.PostRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.DeletePostRequest;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.PostResponse;
import ru.job4j.socialmediaapi.posts.internal.domain.exception.PostNotFoundException;
import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Сценарий удаления поста")
class DeletePostTest {

    /** Время создания поста. */
    private static final Instant CREATED_AT = Instant.parse("2026-10-15T10:00:00Z");

    /** Текущее время сценария. */
    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");

    /** Пост. */
    private static final UUID POST_ID = UUID.randomUUID();

    /** Автор поста. */
    private static final UUID AUTHOR = UUID.randomUUID();

    /** Мок хранилища постов. */
    @Mock
    private PostRepository postRepository;

    /** Проверяемый сценарий. */
    private DeletePost useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeletePost(postRepository, Mappers.getMapper(PostApplicationMapper.class),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Автор удаляет пост: в порт уходит удаление с текущим временем")
    void whenAuthorDeletesThenDeleted() {
        var request = new DeletePostRequest(POST_ID, AUTHOR, NOW);
        when(postRepository.findPublishedById(POST_ID)).thenReturn(Optional.of(new PostResponse(
                POST_ID, AUTHOR, "Заголовок", "Текст", PostStatus.PUBLISHED, CREATED_AT, CREATED_AT)));
        when(postRepository.deletePost(request)).thenReturn(true);

        useCase.delete(new DeletePostCommand(POST_ID, AUTHOR));

        verify(postRepository).deletePost(request);
    }

    @Test
    @DisplayName("Поста нет или он уже удалён: PostNotFoundException")
    void whenMissingThenNotFound() {
        when(postRepository.findPublishedById(POST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.delete(new DeletePostCommand(POST_ID, AUTHOR)))
                .isInstanceOf(PostNotFoundException.class);
    }
}
