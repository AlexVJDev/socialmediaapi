package ru.job4j.socialmediaapi.subscriptions.internal.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.subscriptions.internal.application.mapper.SubscriptionApplicationMapper;
import ru.job4j.socialmediaapi.subscriptions.internal.application.model.GetFollowersCommand;
import ru.job4j.socialmediaapi.subscriptions.internal.application.model.SubscriptionResult;
import ru.job4j.socialmediaapi.subscriptions.internal.application.port.SubscriptionRepository;

import java.util.List;

/**
 * Reading the followers of a user: the subscriptions to the user, newest first.
 * The existence of the user is not checked: there is no users table yet.
 */
@Service
@RequiredArgsConstructor
public class GetFollowers {

    /** Stores subscriptions. */
    private final SubscriptionRepository subscriptionRepository;

    /** Maps the port responses to the results. */
    private final SubscriptionApplicationMapper mapper;

    /**
     * @param command user whose followers are read
     * @return the subscriptions to the user, empty if nobody is subscribed to the user
     */
    @Transactional(readOnly = true)
    public List<SubscriptionResult> get(GetFollowersCommand command) {
        return mapper.toResults(subscriptionRepository.findByFollowed(command.userId()));
    }
}
