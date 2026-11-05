package ru.job4j.socialmediaapi.friendships.internal.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.friendships.internal.application.exception.IncompleteFriendshipOfferCursorException;
import ru.job4j.socialmediaapi.friendships.internal.application.mapper.FriendshipOfferApplicationMapper;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferPageResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.GetFriendshipOffersCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipOfferRepository;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.FriendshipOfferCursor;

/**
 * Reading a page of all friendship offers in any status, newest first
 * ({@code created_at DESC, id DESC}). Pages are read by a cursor, like the feed:
 * no {@code OFFSET}, every page costs the same whatever its depth. The reader is not checked
 * until authentication (Keycloak) is added.
 */
@Service
@RequiredArgsConstructor
public class GetFriendshipOffers {

    /** Page size when the client does not set it. */
    public static final int DEFAULT_LIMIT = 20;

    /** Maximum page size. */
    public static final int MAX_LIMIT = FriendshipOfferRepository.MAX_LIMIT;

    /** Stores friendship offers. */
    private final FriendshipOfferRepository friendshipOfferRepository;

    /** Maps the port page to the result. */
    private final FriendshipOfferApplicationMapper mapper;

    /**
     * @param command cursor of the previous page (none for the first page) and page size
     * @return the page; its {@code next} is {@code null} if there are no more offers
     * @throws IncompleteFriendshipOfferCursorException only one of {@code afterCreatedAt}, {@code afterId} is set
     * @throws IllegalArgumentException                 {@code limit} is less than 1 or greater than {@link #MAX_LIMIT}
     */
    @Transactional(readOnly = true)
    public FriendshipOfferPageResult get(GetFriendshipOffersCommand command) {
        if ((command.afterCreatedAt() == null) != (command.afterId() == null)) {
            throw new IncompleteFriendshipOfferCursorException();
        }
        if (command.afterId() == null) {
            return mapper.toResult(friendshipOfferRepository.findPage(command.limit()));
        }
        FriendshipOfferCursor after = new FriendshipOfferCursor(command.afterCreatedAt(), command.afterId());
        return mapper.toResult(friendshipOfferRepository.findPageAfter(after, command.limit()));
    }
}
