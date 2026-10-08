package ru.job4j.socialmediaapi.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface SubscriptionRepository {

    CreateSubscriptionResponse createSubscription(
            CreateSubscriptionRequest request
    );

    record CreateSubscriptionRequest(
            UUID id,
            UUID followerId,
            UUID followedId,
            Instant createdAt
    ) {
    }

    record CreateSubscriptionResponse(
            UUID id,
            UUID followerId,
            UUID followedId,
            Instant createdAt
    ) {
    }
}
