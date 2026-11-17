package ru.job4j.socialmediaapi.posts.internal.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.posts.internal.application.mapper.PostApplicationMapper;
import ru.job4j.socialmediaapi.posts.internal.application.model.DeletePostCommand;
import ru.job4j.socialmediaapi.posts.internal.application.port.PostRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.DeletePostRequest;
import ru.job4j.socialmediaapi.posts.internal.domain.exception.PostNotFoundException;
import ru.job4j.socialmediaapi.posts.internal.domain.model.Post;

import java.time.Clock;

/**
 * Логическое удаление поста его автором. Право проверяет доменная модель {@link Post};
 * {@code WHERE author_id/status} в запросе порта защищает от гонки между чтением и записью.
 */
@Service
@RequiredArgsConstructor
public class DeletePost {

    /** Хранит посты. */
    private final PostRepository postRepository;

    /** Переводит ответ порта в доменную модель. */
    private final PostApplicationMapper mapper;

    /** Источник текущего времени. */
    private final Clock clock;

    /**
     * @param command пост и текущий пользователь
     * @throws PostNotFoundException поста нет, он уже удалён или текущий пользователь не его автор
     */
    @Transactional
    public void delete(DeletePostCommand command) {
        Post post = postRepository.findPublishedById(command.postId())
                .map(mapper::toDomain)
                .orElseThrow(() -> new PostNotFoundException(command.postId()));
        Post deleted = post.delete(command.actorId(), clock.instant());
        boolean done = postRepository.deletePost(
                new DeletePostRequest(deleted.id(), command.actorId(), deleted.updatedAt()));
        if (!done) {
            throw new PostNotFoundException(command.postId());
        }
    }
}
