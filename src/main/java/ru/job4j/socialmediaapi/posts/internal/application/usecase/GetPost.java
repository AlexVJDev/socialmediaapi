package ru.job4j.socialmediaapi.posts.internal.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.posts.internal.application.mapper.PostApplicationMapper;
import ru.job4j.socialmediaapi.posts.internal.application.model.GetPostCommand;
import ru.job4j.socialmediaapi.posts.internal.application.model.PostResult;
import ru.job4j.socialmediaapi.posts.internal.application.port.PostRepository;
import ru.job4j.socialmediaapi.posts.internal.domain.exception.PostNotFoundException;

/**
 * Reading a single published post by its id.
 */
@Service
@RequiredArgsConstructor
public class GetPost {

    /** Stores posts. */
    private final PostRepository postRepository;

    /** Maps the port response to the result. */
    private final PostApplicationMapper mapper;

    /**
     * @param command post to read
     * @return the published post
     * @throws PostNotFoundException there is no such post or it is deleted
     */
    @Transactional(readOnly = true)
    public PostResult get(GetPostCommand command) {
        return postRepository.findPublishedById(command.postId())
                .map(mapper::toResult)
                .orElseThrow(() -> new PostNotFoundException(command.postId()));
    }
}
