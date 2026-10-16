package ru.job4j.socialmediaapi.friendships.internal.infrastructure.web.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.job4j.socialmediaapi.shared.web.ErrorResponse;

/**
 * Error codes of the friendship offer endpoints: the constant name is the {@code code}
 * of the response body. The common {@code INTERNAL_ERROR} is in {@code shared.exception}.
 */
public enum ErrorCode {

    /** Invalid request body, missing required fields or an offer to oneself. */
    INVALID_FRIENDSHIP_OFFER_REQUEST(
            HttpStatus.BAD_REQUEST,
            "Некорректное тело запроса, отсутствуют обязательные поля или пользователь предлагает дружбу самому себе"
    ),
    /** The friendship offer already waits for a decision. */
    FRIENDSHIP_OFFER_ALREADY_PENDING(
            HttpStatus.CONFLICT,
            "Предложение дружбы уже отправлено и ожидает решения"
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
    );

    /** HTTP status of the error response. */
    private final HttpStatus status;

    /** Human-readable {@code message} of the error response body. */
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }

    /**
     * @return the response with this error's status and {@code {code, message}} body
     */
    public ResponseEntity<ErrorResponse> response() {
        return ResponseEntity.status(status).body(new ErrorResponse(name(), message));
    }
}
