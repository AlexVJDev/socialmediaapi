package ru.job4j.socialmediaapi.subscriptions.internal.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.job4j.socialmediaapi.subscriptions.internal.application.mapper.SubscriptionApplicationMapper;
import ru.job4j.socialmediaapi.subscriptions.internal.application.model.GetSubscriptionsCommand;
import ru.job4j.socialmediaapi.subscriptions.internal.application.model.SubscriptionResult;
import ru.job4j.socialmediaapi.subscriptions.internal.application.port.SubscriptionRepository;

import java.util.List;

/**
 * Reading the subscriptions of a user: the users the user is subscribed to, newest first.
 * The existence of the user is not checked: there is no users table yet.
 */
@Service
@RequiredArgsConstructor
public class GetSubscriptions {

    /** Stores subscriptions. */
    private final SubscriptionRepository subscriptionRepository;

    /** Maps the port responses to the results. */
    private final SubscriptionApplicationMapper mapper;

    /**
     * @param command user whose subscriptions are read
     * @return the subscriptions, empty if the user is subscribed to nobody
     */
    @Transactional(readOnly = true)
    public List<SubscriptionResult> get(GetSubscriptionsCommand command) {
        return mapper.toResults(subscriptionRepository.findByFollower(command.userId()));
    }
}
