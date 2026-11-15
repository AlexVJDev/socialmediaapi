package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.error;

import org.springframework.http.HttpStatus;
import ru.job4j.socialmediaapi.shared.web.ErrorCode;

/**
 * Error codes of the friendship offer endpoints: the constant name is the {@code code}
 * of the response body. The common {@code VALIDATION_ERROR} and {@code INTERNAL_ERROR}
 * are in {@link ru.job4j.socialmediaapi.shared.web.CommonErrorCode}.
 */
public enum FriendshipOfferErrorCode implements ErrorCode {

    /**
     * The offer of this pair of users (in either direction) already waits for a decision.
     * The body has the error format, but the status is {@code 200}: nothing has changed and
     * the client did nothing wrong, while {@code 204} cannot carry a body (RFC 9110, §15.3.5).
     */
    FRIENDSHIP_OFFER_ALREADY_PENDING(
            HttpStatus.OK,
            "Предложение дружбы уже отправлено"
    ),
    /** There is no such offer, or the user who decides it is not its recipient. */
    FRIENDSHIP_OFFER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Предложение дружбы не найдено"
    ),
    /** The friendship offer has already been accepted. */
    FRIENDSHIP_OFFER_ALREADY_ACCEPTED(
            HttpStatus.CONFLICT,
            "Предложение дружбы уже принято"
    ),
    /** The friendship offer has already been rejected. */
    FRIENDSHIP_OFFER_ALREADY_REJECTED(
            HttpStatus.CONFLICT,
            "Предложение дружбы уже отклонено"
    ),
    /** The offer of this pair of users exists, but its status could not be read after the unique key violation. */
    FRIENDSHIP_OFFER_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "Предложение дружбы для этой пары уже существует"
    );

    /** HTTP status of the error response. */
    private final HttpStatus status;

    /** Human-readable {@code detail} of the error response body. */
    private final String detail;

    FriendshipOfferErrorCode(HttpStatus status, String detail) {
        this.status = status;
        this.detail = detail;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String detail() {
        return detail;
    }
}
