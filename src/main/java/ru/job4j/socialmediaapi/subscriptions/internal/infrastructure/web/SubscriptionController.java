package ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.job4j.socialmediaapi.subscriptions.internal.application.usecase.GetFollowers;
import ru.job4j.socialmediaapi.subscriptions.internal.application.usecase.GetSubscriptions;
import ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.web.mapper.SubscriptionWebMapper;
import ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.web.response.FollowerWebResponse;
import ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.web.response.SubscriptionWebResponse;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints {@code /api/v1/subscribes} and {@code /api/v1/subscribes/followers}.
 * Paths, HTTP methods, parameters and their validation come from {@link SubscriptionApi},
 * generated from the OpenAPI contract ({@code docs/api/paths/subscribes-get.yaml},
 * {@code followers-get.yaml}); the annotations are not repeated here.
 */
@RestController
@RequiredArgsConstructor
public class SubscriptionController implements SubscriptionApi {

    /** Use case that reads the subscriptions of a user. */
    private final GetSubscriptions getSubscriptions;

    /** Use case that reads the followers of a user. */
    private final GetFollowers getFollowers;

    /** Maps the parameters to the scenario commands and the results back. */
    private final SubscriptionWebMapper mapper;

    /**
     * {@code 200} with the users the user is subscribed to, newest subscriptions first;
     * an empty list if there are none. {@code 400} ({@code userId} missing or not a UUID)
     * is answered by {@code SubscriptionExceptionHandler}.
     *
     * @param userId user whose subscriptions are read
     * @return the subscriptions
     */
    @Override
    public ResponseEntity<List<SubscriptionWebResponse>> getSubscriptions(UUID userId) {
        return ResponseEntity.ok(mapper.toSubscriptionResponses(
                getSubscriptions.get(mapper.toSubscriptionsCommand(userId))
        ));
    }

    /**
     * {@code 200} with the users subscribed to the user, newest subscriptions first;
     * an empty list if there are none. {@code 400} ({@code userId} missing or not a UUID)
     * is answered by {@code SubscriptionExceptionHandler}.
     *
     * @param userId user whose followers are read
     * @return the followers
     */
    @Override
    public ResponseEntity<List<FollowerWebResponse>> getFollowers(UUID userId) {
        return ResponseEntity.ok(mapper.toFollowerResponses(
                getFollowers.get(mapper.toFollowersCommand(userId))
        ));
    }
}
