package ru.job4j.socialmediaapi.posts.internal.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.job4j.socialmediaapi.posts.internal.application.mapper.PostApplicationMapper;
import ru.job4j.socialmediaapi.posts.internal.application.model.CreatePostCommand;
import ru.job4j.socialmediaapi.posts.internal.application.model.PostResult;
import ru.job4j.socialmediaapi.posts.internal.application.port.PostRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.CreatePostRequest;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.PostResponse;
import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Сценарий публикации поста")
class CreatePostTest {

    /** Текущее время сценария. */
    private static final Instant NOW = Instant.parse("2026-10-15T10:00:00Z");

    /** Мок хранилища постов. */
    @Mock
    private PostRepository postRepository;

    /** Проверяемый сценарий. */
    private CreatePost useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreatePost(postRepository, Mappers.getMapper(PostApplicationMapper.class),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Пост сохраняется от имени текущего пользователя со статусом PUBLISHED и текущим временем")
    void whenCreateThenPublishedPostOfActor() {
        var actor = UUID.randomUUID();
        when(postRepository.createPost(any())).thenAnswer(invocation -> {
            CreatePostRequest request = invocation.getArgument(0);
            return new PostResponse(request.id(), request.authorId(), request.title(), request.content(),
                    request.status(), request.createdAt(), request.updatedAt());
        });

        var result = useCase.create(new CreatePostCommand(actor, "Заголовок", "Текст"));

        var captor = ArgumentCaptor.forClass(CreatePostRequest.class);
        verify(postRepository).createPost(captor.capture());
        assertThat(captor.getValue().id()).isNotNull();
        assertThat(result).isEqualTo(new PostResult(captor.getValue().id(), actor, "Заголовок", "Текст",
                PostStatus.PUBLISHED, NOW, NOW));
    }
}
