package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.job4j.socialmediaapi.friendships.internal.application.usecase.AcceptFriendshipOffer;
import ru.job4j.socialmediaapi.friendships.internal.application.usecase.RejectFriendshipOffer;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.CreateFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.request.DecideFriendshipOfferWebRequest;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.AcceptFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.CreateFriendshipOfferWebResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.mapper.FriendshipOfferWebMapper;
import ru.job4j.socialmediaapi.friendships.internal.application.usecase.CreateFriendshipOffer;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.response.RejectFriendshipOfferWebResponse;

@RestController
@RequestMapping("/api/v1/friendship-offer")
@RequiredArgsConstructor
public class FriendshipOfferController {

    /** Use case that creates friendship offers. */
    private final CreateFriendshipOffer createFriendshipOffer;

    /** Use case that accepts friendship offers. */
    private final AcceptFriendshipOffer acceptFriendshipOffer;

    /** Use case that rejects friendship offers. */
    private final RejectFriendshipOffer rejectFriendshipOffer;

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
    @PostMapping("/create")
    public ResponseEntity<CreateFriendshipOfferWebResponse> create(
            @Valid @RequestBody CreateFriendshipOfferWebRequest request
    ) {
        var result = createFriendshipOffer.create(mapper.toCommand(request));
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
    @PostMapping("/accept")
    public ResponseEntity<AcceptFriendshipOfferWebResponse> accept(
            @Valid @RequestBody DecideFriendshipOfferWebRequest request
    ) {
        var result = acceptFriendshipOffer.accept(mapper.toAcceptCommand(request));
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
    @PostMapping("/reject")
    public ResponseEntity<RejectFriendshipOfferWebResponse> reject(
            @Valid @RequestBody DecideFriendshipOfferWebRequest request
    ) {
        var result = rejectFriendshipOffer.reject(mapper.toRejectCommand(request));
        return ResponseEntity.ok(mapper.toRejectResponse(result));
    }
}
