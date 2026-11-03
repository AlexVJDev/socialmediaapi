package ru.job4j.socialmediaapi.posts.internal.infrastructure.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.job4j.socialmediaapi.posts.internal.application.usecase.GetFeed;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.web.mapper.PostWebMapper;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.web.request.FeedWebRequest;
import ru.job4j.socialmediaapi.posts.internal.infrastructure.web.response.FeedPageWebResponse;

@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
public class FeedController {

    /** Use case that reads a page of the feed. */
    private final GetFeed getFeed;

    /** Maps web DTOs to the scenario command and the result back. */
    private final PostWebMapper mapper;

    /**
     * {@code 200} with a page of the feed. The next page is requested with
     * {@code afterCreatedAt} and {@code afterId} from {@code next}; {@code next == null} —
     * no more posts. Invalid parameters ({@code 400}) are answered by {@code PostExceptionHandler}.
     *
     * @param request reader, page size and cursor
     * @return the page of the feed
     */
    @GetMapping
    public FeedPageWebResponse get(@Valid @ModelAttribute FeedWebRequest request) {
        return mapper.toResponse(getFeed.get(mapper.toGetFeedCommand(request)));
    }
}
