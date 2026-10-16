package ru.job4j.socialmediaapi.subscriptions.internal.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.job4j.socialmediaapi.subscriptions.api.Subscriptions;
import ru.job4j.socialmediaapi.subscriptions.internal.application.port.SubscriptionRepository;
import ru.job4j.socialmediaapi.subscriptions.internal.application.port.model.CreateSubscriptionRequest;

import java.time.Instant;
import java.util.UUID;

/**
 * Implementation of the module API {@link Subscriptions} over {@link SubscriptionRepository}.
 * Has no own transaction: each operation is one SQL statement and joins the caller's transaction.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionsService implements Subscriptions {

    /** Stores subscriptions. */
    private final SubscriptionRepository subscriptionRepository;

    @Override
    public boolean subscribeIfAbsent(UUID followerId, UUID followedId, Instant createdAt) {
        return subscriptionRepository.createSubscriptionIfAbsent(
                new CreateSubscriptionRequest(UUID.randomUUID(), followerId, followedId, createdAt)
        ).isPresent();
    }

    @Override
    public boolean unsubscribe(UUID followerId, UUID followedId) {
        return subscriptionRepository.deleteSubscription(followerId, followedId);
    }
}
