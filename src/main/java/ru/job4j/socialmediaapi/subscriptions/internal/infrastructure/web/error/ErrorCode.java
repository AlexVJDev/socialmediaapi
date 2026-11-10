package ru.job4j.socialmediaapi.subscriptions.internal.infrastructure.web.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.job4j.socialmediaapi.shared.web.ErrorResponse;

/**
 * Error codes of the subscription endpoints: the constant name is the {@code code}
 * of the response body. The common {@code INTERNAL_ERROR} is in {@code shared.exception}.
 */
public enum ErrorCode {

    /** The {@code userId} query parameter is missing or not a UUID. */
    INVALID_USER_ID(
            HttpStatus.BAD_REQUEST,
            "Некорректный или отсутствующий параметр userId"
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
