package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.job4j.socialmediaapi.shared.web.ErrorResponse;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.error.ErrorCode;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyAcceptedException;
import ru.job4j.socialmediaapi.friendships.internal.application.exception.FriendshipOfferAlreadyExistsException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyPendingException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyRejectedException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferToSelfException;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipRequestNotFoundException;

/**
 * Errors of the friendship offer endpoints → HTTP responses of the API contract.
 * Applies only to {@link FriendshipOfferController}; it is consulted before the common
 * {@code shared.exception.GlobalExceptionHandler}, which answers unexpected errors with {@code 500}.
 */
@RestControllerAdvice(assignableTypes = FriendshipOfferController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FriendshipOfferExceptionHandler {

    /** Logs the duplicate whose status could not be read. */
    private static final Logger LOG = LoggerFactory.getLogger(FriendshipOfferExceptionHandler.class);

    /**
     * An unreadable or invalid body of /api/v1/friendship-offer/* (create, accept, reject):
     * one INVALID_FRIENDSHIP_OFFER_REQUEST code for all of them. Other modules map their
     * own request bodies: this handler is limited to {@link FriendshipOfferController}.
     *
     * @return {@code 400} with the INVALID_FRIENDSHIP_OFFER_REQUEST error body
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            FriendshipOfferToSelfException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRequest() {
        return ErrorCode.INVALID_FRIENDSHIP_OFFER_REQUEST.response();
    }

    /**
     * A repeated or a counter offer while the offer of the pair still waits for a decision:
     * nothing has changed.
     *
     * @return {@code 200} with the FRIENDSHIP_OFFER_ALREADY_PENDING body
     */
    @ExceptionHandler(FriendshipOfferAlreadyPendingException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyPending() {
        return ErrorCode.FRIENDSHIP_OFFER_ALREADY_PENDING.response();
    }

    /**
     * Accept/reject of an offer that does not exist or whose recipient is not the actor:
     * the offer of someone else is not disclosed.
     *
     * @return {@code 404} with the FRIENDSHIP_OFFER_NOT_FOUND error body
     */
    @ExceptionHandler(FriendshipRequestNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound() {
        return ErrorCode.FRIENDSHIP_OFFER_NOT_FOUND.response();
    }

    @ExceptionHandler(FriendshipOfferAlreadyAcceptedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyAccepted() {
        return ErrorCode.FRIENDSHIP_OFFER_ALREADY_ACCEPTED.response();
    }

    @ExceptionHandler(FriendshipOfferAlreadyRejectedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyRejected() {
        return ErrorCode.FRIENDSHIP_OFFER_ALREADY_REJECTED.response();
    }

    /**
     * The service rethrows the duplicate only when the existing offer could not be read
     * after the unique key violation (see {@code CreateFriendshipOffer}): the offer
     * is there, but its status is unknown. Nothing has changed, so the client gets the same
     * answer as for a pending offer; the case is logged because it should not happen.
     *
     * @param exception duplicate reported by the repository
     * @return {@code 200} with the FRIENDSHIP_OFFER_ALREADY_PENDING body
     */
    @ExceptionHandler(FriendshipOfferAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyExists(FriendshipOfferAlreadyExistsException exception) {
        LOG.warn("Friendship offer already exists but its status could not be read: {}",
                exception.getMessage(), exception);
        return ErrorCode.FRIENDSHIP_OFFER_ALREADY_PENDING.response();
    }
}
