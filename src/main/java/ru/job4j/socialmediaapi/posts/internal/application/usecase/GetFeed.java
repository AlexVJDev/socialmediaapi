package ru.job4j.socialmediaapi.posts.internal.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.posts.internal.application.mapper.PostApplicationMapper;
import ru.job4j.socialmediaapi.posts.internal.application.model.FeedPageResult;
import ru.job4j.socialmediaapi.posts.internal.application.model.GetFeedCommand;
import ru.job4j.socialmediaapi.posts.internal.application.port.FeedRepository;
import ru.job4j.socialmediaapi.posts.internal.application.port.model.FeedCursor;

/**
 * Reading a page of the feed: published posts of the authors the reader is subscribed to,
 * newest first. Pages are read by a cursor, see {@code docs/process/Feed.md}.
 */
@Service
@RequiredArgsConstructor
public class GetFeed {

    /** Page size when the client does not set it. */
    public static final int DEFAULT_LIMIT = 20;

    /** Maximum page size. */
    public static final int MAX_LIMIT = FeedRepository.MAX_LIMIT;

    /** Builds the feed. */
    private final FeedRepository feedRepository;

    /** Maps the port page to the result. */
    private final PostApplicationMapper mapper;

    /**
     * @param command reader, cursor of the previous page (none for the first page) and page size
     * @return the page; its {@code next} is {@code null} if the feed has no more posts
     * @throws IllegalArgumentException {@code limit} is less than 1 or greater than {@link #MAX_LIMIT}
     * @throws NullPointerException     only one of {@code afterCreatedAt}, {@code afterId} is set
     */
    @Transactional(readOnly = true)
    public FeedPageResult get(GetFeedCommand command) {
        if (command.afterCreatedAt() == null && command.afterId() == null) {
            return mapper.toResult(feedRepository.findFeed(command.followerId(), command.limit()));
        }
        var after = new FeedCursor(command.afterCreatedAt(), command.afterId());
        return mapper.toResult(feedRepository.findFeedAfter(command.followerId(), after, command.limit()));
    }
}
