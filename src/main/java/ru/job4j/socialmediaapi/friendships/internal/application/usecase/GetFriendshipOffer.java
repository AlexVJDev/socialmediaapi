package ru.job4j.socialmediaapi.friendships.internal.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.friendships.internal.application.mapper.FriendshipOfferApplicationMapper;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.GetFriendshipOfferCommand;
import ru.job4j.socialmediaapi.friendships.internal.application.port.FriendshipOfferRepository;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipRequestNotFoundException;

/**
 * Reading a friendship offer in any status by its id. The reader is not checked
 * until authentication (Keycloak) is added.
 */
@Service
@RequiredArgsConstructor
public class GetFriendshipOffer {

    /** Stores friendship offers. */
    private final FriendshipOfferRepository friendshipOfferRepository;

    /** Maps the port response to the result. */
    private final FriendshipOfferApplicationMapper mapper;

    /**
     * @param command offer to read
     * @return the offer
     * @throws FriendshipRequestNotFoundException there is no such offer
     */
    @Transactional(readOnly = true)
    public FriendshipOfferResult get(GetFriendshipOfferCommand command) {
        return friendshipOfferRepository.findById(command.offerId())
                .map(mapper::toResult)
                .orElseThrow(() -> new FriendshipRequestNotFoundException(command.offerId()));
    }
}
