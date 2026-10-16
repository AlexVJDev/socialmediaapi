package ru.job4j.socialmediaapi.friendships.internal.application.port;

import ru.job4j.socialmediaapi.friendships.internal.application.exception.FriendshipAlreadyExistsException;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipResponse;

public interface FriendshipsRepository {

    /**
     * Saves the friendship of two users stored in canonical order.
     * A pair of users has at most one offer, so accepting it cannot meet an existing
     * friendship of the pair: a duplicate here is a data error, the caller does not
     * catch it and its transaction is rolled back.
     *
     * @param request friendship to save
     * @return the saved friendship
     * @throws FriendshipAlreadyExistsException friendship of these users already exists
     */
    CreateFriendshipResponse createFriendship(
            CreateFriendshipRequest request
    );
}
