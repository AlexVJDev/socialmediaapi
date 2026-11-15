package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.job4j.socialmediaapi.shared.web.ApiErrorResponse;
import ru.job4j.socialmediaapi.shared.web.CommonErrorCode;
import ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.error.FriendshipOfferErrorCode;
import ru.job4j.socialmediaapi.friendships.internal.domain.exception.FriendshipOfferAlreadyAcceptedException;
import ru.job4j.socialmediaapi.friendships.internal.application.exception.FriendshipOfferAlreadyExistsException;
import ru.job4j.socialmediaapi.friendships.internal.application.exception.IncompleteFriendshipOfferCursorException;
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
     * An invalid request of /api/v1/friendship-offer[/*]: one common VALIDATION_ERROR code for all of them.
     * <ul>
     *     <li>body of create, accept, reject: unreadable, invalid or an offer to oneself;</li>
     *     <li>path and query parameters: missing ({@code userId}) or of a wrong type (an id that is not a UUID,
     *     {@code limit} or {@code afterCreatedAt} that cannot be converted);</li>
     *     <li>query parameters of the page of offers: {@code limit} outside 1..100
     *     ({@link ConstraintViolationException} of the {@code @Validated} interface),
     *     only one part of the page cursor.</li>
     * </ul>
     *
     * @return {@code 400} with the VALIDATION_ERROR error body
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            FriendshipOfferToSelfException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class,
            IncompleteFriendshipOfferCursorException.class
    })
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest() {
        return CommonErrorCode.VALIDATION_ERROR.response();
    }

    /**
     * A repeated or a counter offer while the offer of the pair still waits for a decision:
     * nothing has changed.
     *
     * @return {@code 200} with the FRIENDSHIP_OFFER_ALREADY_PENDING body
     */
    @ExceptionHandler(FriendshipOfferAlreadyPendingException.class)
    public ResponseEntity<ApiErrorResponse> handleAlreadyPending() {
        return FriendshipOfferErrorCode.FRIENDSHIP_OFFER_ALREADY_PENDING.response();
    }

    /**
     * Accept/reject of an offer that does not exist or whose recipient is not the actor
     * (the offer of someone else is not disclosed); reading an offer that does not exist.
     *
     * @return {@code 404} with the FRIENDSHIP_OFFER_NOT_FOUND error body
     */
    @ExceptionHandler(FriendshipRequestNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound() {
        return FriendshipOfferErrorCode.FRIENDSHIP_OFFER_NOT_FOUND.response();
    }

    @ExceptionHandler(FriendshipOfferAlreadyAcceptedException.class)
    public ResponseEntity<ApiErrorResponse> handleAlreadyAccepted() {
        return FriendshipOfferErrorCode.FRIENDSHIP_OFFER_ALREADY_ACCEPTED.response();
    }

    @ExceptionHandler(FriendshipOfferAlreadyRejectedException.class)
    public ResponseEntity<ApiErrorResponse> handleAlreadyRejected() {
        return FriendshipOfferErrorCode.FRIENDSHIP_OFFER_ALREADY_REJECTED.response();
    }

    /**
     * The service rethrows the duplicate only when the existing offer could not be read
     * after the unique key violation (see {@code CreateFriendshipOffer}): the offer
     * is there, but its status is unknown, so the client gets a conflict: an offer of the pair
     * already exists. The case is logged because it should not happen.
     *
     * @param exception duplicate reported by the repository
     * @return {@code 409} with the FRIENDSHIP_OFFER_ALREADY_EXISTS error body
     */
    @ExceptionHandler(FriendshipOfferAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleAlreadyExists(FriendshipOfferAlreadyExistsException exception) {
        LOG.warn("Friendship offer already exists but its status could not be read: {}",
                exception.getMessage(), exception);
        return FriendshipOfferErrorCode.FRIENDSHIP_OFFER_ALREADY_EXISTS.response();
    }
}
