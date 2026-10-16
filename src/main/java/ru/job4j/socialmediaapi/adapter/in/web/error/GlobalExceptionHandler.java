package ru.job4j.socialmediaapi.adapter.in.web.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.job4j.socialmediaapi.shared.web.ErrorResponse;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferAlreadyAcceptedException;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferAlreadyExistsException;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferAlreadyPendingException;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferAlreadyRejectedException;
import ru.job4j.socialmediaapi.domain.friendshipoffer.exception.FriendshipOfferToSelfException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Logs unexpected errors that are hidden from the client. */
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * An unreadable or invalid body is so far possible only for /api/offer-friendships/*,
     * and accept/reject use the same INVALID_FRIENDSHIP_OFFER_REQUEST code as create.
     * Map these exceptions per endpoint once other request bodies appear.
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

    @ExceptionHandler(FriendshipOfferAlreadyPendingException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyPending() {
        return ErrorCode.FRIENDSHIP_OFFER_ALREADY_PENDING.response();
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
     * after the unique key violation (see {@code FriendshipOfferService}): the offer
     * is there, but its status is unknown. Nothing has changed, so the client gets
     * {@code 204} without a body; the case is logged because it should not happen.
     *
     * @param exception duplicate reported by the repository
     * @return {@code 204} without a body
     */
    @ExceptionHandler(FriendshipOfferAlreadyExistsException.class)
    public ResponseEntity<Void> handleAlreadyExists(FriendshipOfferAlreadyExistsException exception) {
        LOG.warn("Friendship offer already exists but its status could not be read: {}",
                exception.getMessage(), exception);
        return ResponseEntity.noContent().build();
    }

    /**
     * A database constraint fired although the application checks the rules before:
     * this is a bug, not a business outcome. The PostgreSQL message (with the
     * constraint name) goes to the log, the client gets a generic body.
     *
     * @param exception violation reported by Spring JDBC
     * @return {@code 500} with the INTERNAL_ERROR error body
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        LOG.error("Unexpected database constraint violation: {}",
                exception.getMostSpecificCause().getMessage(), exception);
        return ErrorCode.INTERNAL_ERROR.response();
    }
}
