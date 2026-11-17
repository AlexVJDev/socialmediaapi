package ru.job4j.socialmediaapi.posts.internal.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.posts.internal.application.exception.IncompleteFeedCursorException;
import ru.job4j.socialmediaapi.posts.internal.application.mapper.PostApplicationMapper;
import ru.job4j.socialmediaapi.posts.internal.application.model.FeedPageResult;
import ru.job4j.socialmediaapi.posts.internal.application.model.GetUserPostsCommand;
import ru.job4j.socialmediaapi.posts.internal.application.port.PostRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedCursor;

/**
 * Чтение страницы опубликованных постов пользователя, от новых к старым.
 * Страницы читаются по курсору, как лента.
 */
@Service
@RequiredArgsConstructor
public class GetUserPosts {

    /** Хранит посты. */
    private final PostRepository postRepository;

    /** Переводит страницу порта в результат. */
    private final PostApplicationMapper mapper;

    /**
     * @param command автор, курсор предыдущей страницы (для первой — нет) и размер страницы
     * @return страница; {@code next} равен {@code null}, если постов больше нет
     * @throws IncompleteFeedCursorException задана только одна часть курсора ({@code afterCreatedAt}, {@code afterId})
     * @throws IllegalArgumentException      {@code limit} меньше 1 или больше {@link GetFeed#MAX_LIMIT}
     */
    @Transactional(readOnly = true)
    public FeedPageResult get(GetUserPostsCommand command) {
        if ((command.afterCreatedAt() == null) != (command.afterId() == null)) {
            throw new IncompleteFeedCursorException();
        }
        if (command.afterId() == null) {
            return mapper.toResult(postRepository.findPublishedByAuthor(command.authorId(), command.limit()));
        }
        FeedCursor after = new FeedCursor(command.afterCreatedAt(), command.afterId());
        return mapper.toResult(
                postRepository.findPublishedByAuthorAfter(command.authorId(), after, command.limit()));
    }
}
