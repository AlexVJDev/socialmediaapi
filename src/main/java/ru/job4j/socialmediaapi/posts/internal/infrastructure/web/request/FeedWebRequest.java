package ru.job4j.socialmediaapi.posts.internal.infrastructure.web.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import ru.job4j.socialmediaapi.posts.internal.application.usecase.GetFeed;

import java.time.Instant;
import java.util.UUID;

/**
 * Query parameters of {@code GET /api/v1/feed}. The cursor ({@code afterCreatedAt} and
 * {@code afterId}) is copied from {@code next} of the previous page; both are set or neither.
 *
 * @param followerId     reader of the feed (until Keycloak is connected it is passed by the client)
 * @param limit          page size, from 1 to {@link GetFeed#MAX_LIMIT}; {@link GetFeed#DEFAULT_LIMIT} if absent
 * @param afterCreatedAt {@code next.createdAt} of the previous page
 * @param afterId        {@code next.id} of the previous page
 */
public record FeedWebRequest(
        @NotNull UUID followerId,
        @Min(1) @Max(GetFeed.MAX_LIMIT) Integer limit,
        Instant afterCreatedAt,
        UUID afterId
) {

    /**
     * Sets the default page size.
     */
    public FeedWebRequest {
        if (limit == null) {
            limit = GetFeed.DEFAULT_LIMIT;
        }
    }

    /**
     * @return {@code true} if both parts of the cursor are set or neither
     */
    @AssertTrue(message = "afterCreatedAt and afterId must be set together")
    public boolean isCursorComplete() {
        return (afterCreatedAt == null) == (afterId == null);
    }
}
