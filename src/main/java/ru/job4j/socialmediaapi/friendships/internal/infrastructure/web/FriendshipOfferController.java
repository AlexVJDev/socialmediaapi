package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.job4j.socialmediaapi.friendships.internal.application.model.CreateFriendshipOfferResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferDecisionResult;
import ru.job4j.socialmediaapi.friendships.internal.application.model.FriendshipOfferPageResult;
import ru.job4j.socialmediaapi.friendships.internal.application.usecase.AcceptFriendshipOffer;
import ru.job4j.socialmediaapi.friendships.internal.application.usecase.CreateFriendshipOffer;
import ru.job4j.socialmediaapi.friendships.internal.application.usecase.GetFriendshipOffer;
import ru.job4j.socialmediaapi.friendships.internal.application.usecase.GetFriendshipOffers;
import ru.job4j.socialmediaapi.friendships.internal.application.usecase.RejectFriendshipOffer;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.mapper.FriendshipOfferWebMapper;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.AcceptFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.CreateFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.RejectFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.AcceptFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.CreateFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.FriendshipOfferPageWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.FriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.RejectFriendshipOfferWebResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Endpoints {@code /api/v1/friendship-offer} and {@code /api/v1/friendship-offer/*}.
 * Paths, HTTP methods, parameters and their validation come from {@link FriendshipOfferApi},
 * generated from the OpenAPI contract ({@code docs/api/paths/friendship-offer*.yaml});
 * the annotations are not repeated here.
 */
@RestController
@RequiredArgsConstructor
public class FriendshipOfferController implements FriendshipOfferApi {

    /** Use case that creates friendship offers. */
    private final CreateFriendshipOffer createFriendshipOffer;

    /** Use case that accepts friendship offers. */
    private final AcceptFriendshipOffer acceptFriendshipOffer;

    /** Use case that rejects friendship offers. */
    private final RejectFriendshipOffer rejectFriendshipOffer;

    /** Use case that reads one offer. */
    private final GetFriendshipOffer getFriendshipOffer;

    /** Use case that reads a page of all offers. */
    private final GetFriendshipOffers getFriendshipOffers;

    /** Maps web DTOs to the scenario command and the result back. */
    private final FriendshipOfferWebMapper mapper;

    /**
     * {@code 201} with the new offer. A repeated or a counter offer is answered by
     * {@code FriendshipOfferExceptionHandler} by the status of the pair's offer:
     * {@code PENDING} → {@code 200} FRIENDSHIP_OFFER_ALREADY_PENDING,
     * {@code ACCEPTED} / {@code REJECTED} → {@code 409} with the code of the status.
     *
     * @param request sender and recipient of the offer
     * @return the created offer
     */
    @Override
    public ResponseEntity<CreateFriendshipOfferWebResponse> createFriendshipOffer(
            CreateFriendshipOfferWebRequest request
    ) {
        CreateFriendshipOfferResult result = createFriendshipOffer.create(mapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(result));
    }

    /**
     * {@code 200} with the accepted offer; the friendship and the back subscription are created
     * in the same transaction. Errors ({@code 404} — no offer or not its recipient, {@code 409} —
     * already decided) are answered by {@code FriendshipOfferExceptionHandler}.
     *
     * @param request offer and the user who accepts it
     * @return the accepted offer
     */
    @Override
    public ResponseEntity<AcceptFriendshipOfferWebResponse> acceptFriendshipOffer(
            AcceptFriendshipOfferWebRequest request
    ) {
        FriendshipOfferDecisionResult result = acceptFriendshipOffer.accept(mapper.toAcceptCommand(request));
        return ResponseEntity.ok(mapper.toAcceptResponse(result));
    }

    /**
     * {@code 200} with the rejected offer; no friendship is created, the sender stays subscribed.
     * Errors ({@code 404} — no offer or not its recipient, {@code 409} — already decided)
     * are answered by {@code FriendshipOfferExceptionHandler}.
     *
     * @param request offer and the user who rejects it
     * @return the rejected offer
     */
    @Override
    public ResponseEntity<RejectFriendshipOfferWebResponse> rejectFriendshipOffer(
            RejectFriendshipOfferWebRequest request
    ) {
        FriendshipOfferDecisionResult result = rejectFriendshipOffer.reject(mapper.toRejectCommand(request));
        return ResponseEntity.ok(mapper.toRejectResponse(result));
    }

    /**
     * {@code 200} with the offer in any status; {@code 404} (no such offer) and {@code 400}
     * (the id is not a UUID) are answered by {@code FriendshipOfferExceptionHandler}.
     *
     * @param offerId offer id
     * @return the offer
     */
    @Override
    public ResponseEntity<FriendshipOfferWebResponse> getFriendshipOffer(UUID offerId) {
        return ResponseEntity.ok(mapper.toResponse(getFriendshipOffer.get(mapper.toGetCommand(offerId))));
    }

    /**
     * {@code 200} with a page of all offers, newest first. The next page is requested with
     * {@code afterCreatedAt} and {@code afterId} from {@code next}; {@code next == null} — no more offers.
     * Invalid parameters ({@code 400}) are answered by {@code FriendshipOfferExceptionHandler}.
     *
     * @param limit          page size, from 1 to 100 (20 if not set)
     * @param afterCreatedAt {@code next.createdAt} of the previous page
     * @param afterId        {@code next.id} of the previous page
     * @return the page of offers
     */
    @Override
    public ResponseEntity<FriendshipOfferPageWebResponse> getFriendshipOffers(
            Integer limit, Instant afterCreatedAt, UUID afterId
    ) {
        FriendshipOfferPageResult result =
                getFriendshipOffers.get(mapper.toGetPageCommand(limit, afterCreatedAt, afterId));
        return ResponseEntity.ok(mapper.toResponse(result));
    }

    /**
     * Not implemented yet.
     *
     * @param userId recipient of the offers
     * @return {@code 501 Not Implemented}
     */
    @Override
    public ResponseEntity<List<FriendshipOfferWebResponse>> getIncomingFriendshipOffers(UUID userId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * Not implemented yet.
     *
     * @param userId sender of the offers
     * @return {@code 501 Not Implemented}
     */
    @Override
    public ResponseEntity<List<FriendshipOfferWebResponse>> getOutgoingFriendshipOffers(UUID userId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
