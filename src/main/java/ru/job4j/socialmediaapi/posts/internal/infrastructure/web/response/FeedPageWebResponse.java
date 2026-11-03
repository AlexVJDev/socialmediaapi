package ru.job4j.socialmediaapi.posts.internal.infrastructure.web.response;

import java.util.List;

public record FeedPageWebResponse(
        List<PostWebResponse> posts,
        FeedCursorWebResponse next
) {
}
