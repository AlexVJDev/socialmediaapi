package ru.job4j.socialmediaapi.posts.internal.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.posts.internal.application.mapper.PostApplicationMapper;
import ru.job4j.socialmediaapi.posts.internal.application.model.CreatePostCommand;
import ru.job4j.socialmediaapi.posts.internal.application.model.PostResult;
import ru.job4j.socialmediaapi.posts.internal.application.port.PostRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.CreatePostRequest;
import ru.job4j.socialmediaapi.posts.internal.domain.model.PostStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Публикация поста от имени текущего пользователя: id и время задаёт сценарий, статус — {@code PUBLISHED}.
 */
@Service
@RequiredArgsConstructor
public class CreatePost {

    /** Хранит посты. */
    private final PostRepository postRepository;

    /** Переводит ответ порта в результат. */
    private final PostApplicationMapper mapper;

    /** Источник текущего времени. */
    private final Clock clock;

    /**
     * @param command автор, заголовок и текст
     * @return опубликованный пост, как он сохранён
     */
    @Transactional
    public PostResult create(CreatePostCommand command) {
        Instant now = clock.instant();
        CreatePostRequest request = new CreatePostRequest(
                UUID.randomUUID(),
                command.actorId(),
                command.title(),
                command.content(),
                PostStatus.PUBLISHED,
                now,
                now
        );
        return mapper.toResult(postRepository.createPost(request));
    }
}
