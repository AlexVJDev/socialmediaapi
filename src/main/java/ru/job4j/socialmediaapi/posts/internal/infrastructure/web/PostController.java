package ru.job4j.socialmediaapi.posts.internal.infrastructure.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.job4j.socialmediaapi.posts.internal.application.usecase.GetPost;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.web.mapper.PostWebMapper;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.web.response.PostWebResponse;

import java.util.UUID;

/**
 * Endpoint {@code GET /api/v1/posts/{postId}}.
 * The path, HTTP method and parameter come from {@link PostApi}, generated from the OpenAPI contract
 * ({@code docs/api/paths/post-get.yaml}); the annotations are not repeated here.
 */
@RestController
@RequiredArgsConstructor
public class PostController implements PostApi {

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
    @Override
    public ResponseEntity<PostWebResponse> getPost(UUID postId) {
        return ResponseEntity.ok(mapper.toResponse(getPost.get(mapper.toGetPostCommand(postId))));
    }
}
