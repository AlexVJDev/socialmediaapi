package ru.job4j.socialmediaapi.friendships.internal.application.port;

import ru.job4j.socialmediaapi.friendships.internal.application.exception.FriendshipAlreadyExistsException;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipRequest;
import ru.job4j.socialmediaapi.friendships.internal.application.port.model.CreateFriendshipResponse;

public interface FriendshipsRepository {

    /**
     * Saves the friendship of two users stored in canonical order.
     *
     * @param request friendship to save
     * @return the saved friendship
     * @throws FriendshipAlreadyExistsException friendship of these users already exists
     */
    CreateFriendshipResponse createFriendship(
            CreateFriendshipRequest request
    );
}
