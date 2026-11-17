package ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.job4j.socialmediaapi.subscriptions.internal.application.usecase.GetFollowers;
import ru.job4j.socialmediaapi.shared.security.CurrentUserProvider;
import ru.job4j.socialmediaapi.subscriptions.internal.application.model.SubscribeResult;
import ru.job4j.socialmediaapi.subscriptions.internal.application.usecase.GetSubscriptions;
import ru.job4j.socialmediaapi.subscriptions.internal.application.usecase.Subscribe;
import ru.job4j.socialmediaapi.subscriptions.internal.application.usecase.Unsubscribe;
import ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.web.mapper.SubscriptionWebMapper;
import ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.web.request.SubscribeWebRequest;

import java.util.UUID;

/**
 * Endpoints {@code /api/v1/subscribes}, {@code /api/v1/subscribes/{userId}} and {@code /api/v1/subscribes/followers}.
 * Paths, HTTP methods, parameters and their validation come from {@link SubscriptionApi},
 * generated from the OpenAPI contract ({@code docs/api/paths/subscribes.yaml},
 * {@code subscribe-delete.yaml}, {@code followers-get.yaml}); the annotations are not repeated here.
 * <p>
 * Подписчик при подписке и отписке — текущий пользователь из {@link CurrentUserProvider};
 * списки подписок и подписчиков читаются по параметру {@code userId}.
 */
@RestController
@RequiredArgsConstructor
public class SubscriptionController implements SubscriptionApi {

    /** Use case that reads the subscriptions of a user. */
    private final GetSubscriptions getSubscriptions;

    /** Use case that reads the followers of a user. */
    private final GetFollowers getFollowers;

    /** Подписывает текущего пользователя. */
    private final Subscribe subscribe;

    /** Отписывает текущего пользователя. */
    private final Unsubscribe unsubscribe;

    /** Текущий пользователь. */
    private final CurrentUserProvider currentUser;

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
    public ResponseEntity<?> getSubscriptions(UUID userId) {
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
    public ResponseEntity<?> getFollowers(UUID userId) {
        return ResponseEntity.ok(mapper.toFollowerResponses(
                getFollowers.get(mapper.toFollowersCommand(userId))
        ));
    }

    /**
     * {@code 201} с новой подпиской или {@code 200} с уже существующей.
     * Подписка на себя и некорректное тело ({@code 400}) отвечает {@code SubscriptionExceptionHandler}.
     *
     * @param request пользователь, на которого подписывается текущий пользователь
     * @return подписка
     */
    @Override
    public ResponseEntity<?> subscribe(SubscribeWebRequest request) {
        SubscribeResult result = subscribe.subscribe(
                mapper.toSubscribeCommand(currentUser.currentUserId(), request));
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(mapper.toSubscriptionResponse(result.subscription()));
    }

    /**
     * {@code 204}, даже если подписки не было.
     *
     * @param userId пользователь, от которого отписывается текущий пользователь
     * @return пустой ответ
     */
    @Override
    public ResponseEntity<?> unsubscribe(UUID userId) {
        unsubscribe.unsubscribe(mapper.toUnsubscribeCommand(currentUser.currentUserId(), userId));
        return ResponseEntity.noContent().build();
    }
}
