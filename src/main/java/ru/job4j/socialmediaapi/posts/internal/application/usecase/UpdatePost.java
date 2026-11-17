package ru.job4j.socialmediaapi.posts.internal.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.posts.internal.application.mapper.PostApplicationMapper;
import ru.job4j.socialmediaapi.posts.internal.application.model.PostResult;
import ru.job4j.socialmediaapi.posts.internal.application.model.UpdatePostCommand;
import ru.job4j.socialmediaapi.posts.internal.application.port.PostRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.UpdatePostRequest;
import ru.job4j.socialmediaapi.posts.internal.domain.exception.PostNotFoundException;
import ru.job4j.socialmediaapi.posts.internal.domain.model.Post;

import java.time.Clock;

/**
 * Изменение заголовка и текста поста его автором. Право проверяет доменная модель {@link Post};
 * {@code WHERE author_id/status} в запросе порта защищает от гонки между чтением и записью.
 */
@Service
@RequiredArgsConstructor
public class UpdatePost {

    /** Хранит посты. */
    private final PostRepository postRepository;

    /** Переводит ответ порта в доменную модель и результат. */
    private final PostApplicationMapper mapper;

    /** Источник текущего времени. */
    private final Clock clock;

    /**
     * @param command пост, текущий пользователь, новые заголовок и текст
     * @return изменённый пост
     * @throws PostNotFoundException поста нет, он удалён или текущий пользователь не его автор
     */
    @Transactional
    public PostResult update(UpdatePostCommand command) {
        Post post = postRepository.findPublishedById(command.postId())
                .map(mapper::toDomain)
                .orElseThrow(() -> new PostNotFoundException(command.postId()));
        Post updated = post.update(command.actorId(), command.title(), command.content(), clock.instant());
        UpdatePostRequest request = new UpdatePostRequest(
                updated.id(),
                command.actorId(),
                updated.title(),
                updated.content(),
                updated.updatedAt()
        );
        return postRepository.updatePost(request)
                .map(mapper::toResult)
                .orElseThrow(() -> new PostNotFoundException(command.postId()));
    }
}
