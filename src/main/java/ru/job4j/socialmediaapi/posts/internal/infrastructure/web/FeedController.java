package ru.job4j.socialmediaapi.posts.internal.infrastructure.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.job4j.socialmediaapi.posts.internal.application.usecase.GetFeed;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.web.mapper.PostWebMapper;
import ru.job4j.socialmediaapi.shared.security.CurrentUserProvider;

import java.time.Instant;
import java.util.UUID;

/**
 * Endpoint {@code GET /api/v1/feed}.
 * The path, HTTP method, parameters and their validation ({@code limit} 1..100, default 20) come from
 * {@link FeedApi}, generated from the OpenAPI contract ({@code docs/api/paths/feed-get.yaml});
 * the annotations are not repeated here.
 * <p>
 * Читатель ленты — текущий пользователь из {@link CurrentUserProvider}, не параметр запроса.
 */
@RestController
@RequiredArgsConstructor
public class FeedController implements FeedApi {

    /** Use case that reads a page of the feed. */
    private final GetFeed getFeed;

    /** Текущий пользователь — читатель ленты. */
    private final CurrentUserProvider currentUser;

    /** Maps web DTOs to the scenario command and the result back. */
    private final PostWebMapper mapper;

    /**
     * {@code 200} with a page of the feed. The next page is requested with
     * {@code afterCreatedAt} and {@code afterId} from {@code next}; {@code next == null} —
     * no more posts. Invalid parameters ({@code 400}) are answered by {@code PostExceptionHandler}.
     *
     * @param limit          page size, from 1 to 100 (20 if not set)
     * @param afterCreatedAt {@code next.createdAt} of the previous page
     * @param afterId        {@code next.id} of the previous page
     * @return the page of the feed
     */
    @Override
    public ResponseEntity<?> getFeed(
            Integer limit, Instant afterCreatedAt, UUID afterId
    ) {
        UUID followerId = currentUser.currentUserId();
        return ResponseEntity.ok(mapper.toResponse(
                getFeed.get(mapper.toGetFeedCommand(followerId, limit, afterCreatedAt, afterId))
        ));
    }
}
