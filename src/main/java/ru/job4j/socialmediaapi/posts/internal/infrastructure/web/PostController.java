package ru.job4j.socialmediaapi.posts.internal.infrastructure.web;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.job4j.socialmediaapi.posts.internal.application.usecase.GetPost;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.web.mapper.PostWebMapper;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.web.response.PostWebResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    /** Use case that reads a post. */
    private final GetPost getPost;

    /** Maps web DTOs to the scenario command and the result back. */
    private final PostWebMapper mapper;

    /**
     * {@code 200} with the published post. A missing or deleted post ({@code 404}) and an id
     * that is not a UUID ({@code 400}) are answered by {@code PostExceptionHandler}.
     *
     * @param postId id of the post
     * @return the post
     */
    @GetMapping("/{postId}")
    public PostWebResponse get(@PathVariable UUID postId) {
        return mapper.toResponse(getPost.get(mapper.toGetPostCommand(postId)));
    }
}
