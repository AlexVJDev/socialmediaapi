package ru.job4j.socialmediaapi.posts.internal.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.job4j.socialmediaapi.posts.internal.application.mapper.PostApplicationMapper;
import ru.job4j.socialmediaapi.posts.internal.application.model.PostResult;
import ru.job4j.socialmediaapi.posts.internal.application.model.UpdatePostCommand;
import ru.job4j.socialmediaapi.posts.internal.application.port.PostRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.PostResponse;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.UpdatePostRequest;
import ru.job4j.socialmediaapi.posts.internal.domain.exception.PostNotFoundException;
import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Сценарий изменения поста")
class UpdatePostTest {

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
    private UpdatePost useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdatePost(postRepository, Mappers.getMapper(PostApplicationMapper.class),
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(postRepository.findPublishedById(POST_ID)).thenReturn(Optional.of(new PostResponse(
                POST_ID, AUTHOR, "Заголовок", "Текст", PostStatus.PUBLISHED, CREATED_AT, CREATED_AT)));
    }

    @Test
    @DisplayName("Автор меняет пост: в порт уходят новые поля и текущее время, возвращается сохранённый пост")
    void whenAuthorUpdatesThenUpdatedPost() {
        var request = new UpdatePostRequest(POST_ID, AUTHOR, "Новый", "Новый текст", NOW);
        when(postRepository.updatePost(request)).thenReturn(Optional.of(new PostResponse(
                POST_ID, AUTHOR, "Новый", "Новый текст", PostStatus.PUBLISHED, CREATED_AT, NOW)));

        var result = useCase.update(new UpdatePostCommand(POST_ID, AUTHOR, "Новый", "Новый текст"));

        assertThat(result).isEqualTo(new PostResult(
                POST_ID, AUTHOR, "Новый", "Новый текст", PostStatus.PUBLISHED, CREATED_AT, NOW));
    }

    @Test
    @DisplayName("Чужой пост: PostNotFoundException, в порт ничего не пишется")
    void whenOtherUserThenNotFound() {
        assertThatThrownBy(() -> useCase.update(
                new UpdatePostCommand(POST_ID, UUID.randomUUID(), "Новый", "Новый текст")))
                .isInstanceOf(PostNotFoundException.class);
        verify(postRepository, never()).updatePost(any());
    }
}
